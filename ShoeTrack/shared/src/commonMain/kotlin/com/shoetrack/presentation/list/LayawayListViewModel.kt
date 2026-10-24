package com.shoetrack.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoetrack.domain.model.LayawaySummary
import com.shoetrack.domain.usecase.ObserveLayawaysUseCase
import com.shoetrack.domain.usecase.RefreshOverdueUseCase
import com.shoetrack.presentation.common.runCatchingExceptCancellation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LayawayListUiState {
    data object Loading : LayawayListUiState
    data class Loaded(val layaways: List<LayawaySummary>) : LayawayListUiState
    data object Error : LayawayListUiState
}

class LayawayListViewModel(
    observeLayaways: ObserveLayawaysUseCase,
    private val refreshOverdue: RefreshOverdueUseCase,
) : ViewModel() {

    val uiState: StateFlow<LayawayListUiState> = observeLayaways()
        .map<List<LayawaySummary>, LayawayListUiState> { LayawayListUiState.Loaded(it) }
        .catch { emit(LayawayListUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LayawayListUiState.Loading)

    init {
        viewModelScope.launch { runCatchingExceptCancellation { refreshOverdue() } }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
