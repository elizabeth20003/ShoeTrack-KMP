package com.shoetrack.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoetrack.domain.model.LayawaySummary
import com.shoetrack.domain.usecase.ObserveLayawaysUseCase
import com.shoetrack.domain.usecase.RefreshOverdueUseCase
import com.shoetrack.presentation.common.runCatchingExceptCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LayawayListUiState {
    data object Loading : LayawayListUiState
    /** [layaways] are the ones matching the search; [hasAnyLayaways] tells an empty store from an empty search. */
    data class Loaded(
        val layaways: List<LayawaySummary>,
        val hasAnyLayaways: Boolean,
    ) : LayawayListUiState
    data object Error : LayawayListUiState
}

class LayawayListViewModel(
    observeLayaways: ObserveLayawaysUseCase,
    private val refreshOverdue: RefreshOverdueUseCase,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val uiState: StateFlow<LayawayListUiState> = combine<List<LayawaySummary>, String, LayawayListUiState>(observeLayaways(), _query) { layaways, query ->
        LayawayListUiState.Loaded(
            layaways = layaways.filter { it.matches(query) },
            hasAnyLayaways = layaways.isNotEmpty(),
        )
    }
        .catch { emit(LayawayListUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LayawayListUiState.Loading)

    fun onQueryChange(query: String) {
        _query.value = query
    }

    init {
        viewModelScope.launch { runCatchingExceptCancellation { refreshOverdue() } }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
