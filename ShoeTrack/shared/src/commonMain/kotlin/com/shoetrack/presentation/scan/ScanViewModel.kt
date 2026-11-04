package com.shoetrack.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoetrack.domain.notebook.NotebookEntry
import com.shoetrack.domain.notebook.NotebookParser
import com.shoetrack.domain.repository.AiKeyRepository
import com.shoetrack.domain.service.NotebookReader
import com.shoetrack.domain.service.TextRecognizer
import com.shoetrack.presentation.common.runCatchingExceptCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class ScanUiState(
    val isReading: Boolean = false,
    /** The raw text read from the last photo, shown when no layaway could be found in it. */
    val text: String? = null,
    val hasFailed: Boolean = false,
    val foundNothing: Boolean = false,
    /** Whether an API key is saved, so photos are read by the cloud service first. */
    val hasAiKey: Boolean = false,
    /** The cloud service could not be used for the last photo, so the phone read it instead. */
    val aiUnavailable: Boolean = false,
)

sealed interface ScanEvent {
    /** A layaway line was found; the form opens with what could be read of it. */
    data class EntryRead(val entry: NotebookEntry) : ScanEvent
}

class ScanViewModel(
    private val recognizer: TextRecognizer,
    private val cloudReader: NotebookReader,
    private val keys: AiKeyRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _events = Channel<ScanEvent>(Channel.BUFFERED)
    val events: Flow<ScanEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { _uiState.update { it.copy(hasAiKey = keys.key() != null) } }
    }

    fun onAiKeySaved(key: String) {
        viewModelScope.launch {
            keys.save(key)
            _uiState.update { it.copy(hasAiKey = keys.key() != null) }
        }
    }

    fun onAiKeyRemoved() = onAiKeySaved("")

    fun onPhotoPicked(image: ByteArray) {
        val hasAiKey = _uiState.value.hasAiKey
        _uiState.update { ScanUiState(isReading = true, hasAiKey = hasAiKey) }
        viewModelScope.launch {
            val cloud = if (hasAiKey) runCatchingExceptCancellation { cloudReader.read(image) } else null
            val cloudEntry = cloud?.getOrNull()
            if (cloudEntry != null) {
                finish(cloudEntry, text = null, hasFailed = false, hasAiKey = true, aiUnavailable = false)
            } else {
                val result = runCatchingExceptCancellation { recognizer.recognize(image) }
                val text = result.getOrNull()
                val entry = text?.let { NotebookParser.parse(it) }
                finish(entry, text, result.isFailure, hasAiKey, aiUnavailable = cloud != null)
            }
        }
    }

    private suspend fun finish(
        entry: NotebookEntry?,
        text: String?,
        hasFailed: Boolean,
        hasAiKey: Boolean,
        aiUnavailable: Boolean,
    ) {
        val checked = entry?.withoutFutureDate()
        val foundNothing = checked == NotebookEntry()
        _uiState.value = ScanUiState(
            text = text,
            hasFailed = hasFailed,
            foundNothing = foundNothing,
            hasAiKey = hasAiKey,
            aiUnavailable = aiUnavailable,
        )
        if (checked != null && !foundNothing) _events.send(ScanEvent.EntryRead(checked))
    }

    // A date read after today is a misread digit, and a layaway cannot start in the future.
    private fun NotebookEntry.withoutFutureDate(): NotebookEntry {
        val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return if (lastPaymentDate != null && lastPaymentDate > today) copy(lastPaymentDate = null) else this
    }
}
