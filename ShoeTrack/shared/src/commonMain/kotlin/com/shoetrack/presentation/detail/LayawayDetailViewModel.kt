package com.shoetrack.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoetrack.domain.error.LayawayNotFoundException
import com.shoetrack.domain.model.LayawayDetail
import com.shoetrack.domain.usecase.GetLayawayDetailUseCase
import com.shoetrack.domain.usecase.RegisterPaymentUseCase
import com.shoetrack.presentation.common.runCatchingExceptCancellation
import com.shoetrack.presentation.format.MAX_MONEY_DIGITS
import com.shoetrack.presentation.format.toMoneyOrNull
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LayawayDetailUiState(
    val detail: LayawayDetail? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isPaymentDialogOpen: Boolean = false,
    val paymentInput: String = "",
    val isSubmittingPayment: Boolean = false,
) {
    val isPaymentTooHigh: Boolean
        get() {
            val amount = paymentInput.toMoneyOrNull() ?: return false
            val balance = detail?.layaway?.remainingBalance ?: return false
            return amount > balance
        }

    val canConfirmPayment: Boolean
        get() = !isSubmittingPayment && !isPaymentTooHigh && paymentInput.toMoneyOrNull()?.isPositive == true
}

sealed interface LayawayDetailEvent {
    data object PaymentFailed : LayawayDetailEvent
}

class LayawayDetailViewModel(
    private val layawayId: String,
    private val getLayawayDetail: GetLayawayDetailUseCase,
    private val registerPayment: RegisterPaymentUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LayawayDetailUiState())
    val uiState: StateFlow<LayawayDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<LayawayDetailEvent>(Channel.BUFFERED)
    val events: Flow<LayawayDetailEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { loadDetail() }
    }

    fun openPaymentDialog() = _uiState.update { it.copy(isPaymentDialogOpen = true) }

    fun dismissPaymentDialog() =
        _uiState.update { it.copy(isPaymentDialogOpen = false, paymentInput = "") }

    fun onPaymentInputChange(value: String) =
        _uiState.update { it.copy(paymentInput = value.filter(Char::isDigit).take(MAX_MONEY_DIGITS)) }

    fun confirmPayment() {
        val state = _uiState.value
        val amount = state.paymentInput.toMoneyOrNull()
        if (!state.canConfirmPayment || amount == null) return

        _uiState.update { it.copy(isSubmittingPayment = true) }
        viewModelScope.launch {
            val result = runCatchingExceptCancellation { registerPayment(layawayId, amount) }
            _uiState.update {
                it.copy(isSubmittingPayment = false, isPaymentDialogOpen = false, paymentInput = "")
            }
            if (result.isSuccess) loadDetail() else _events.send(LayawayDetailEvent.PaymentFailed)
        }
    }

    private suspend fun loadDetail() {
        val result = runCatchingExceptCancellation { getLayawayDetail(layawayId) }
        _uiState.update {
            it.copy(
                detail = result.getOrNull(),
                isLoading = false,
                loadFailed = result.exceptionOrNull().let { error ->
                    error != null && error !is LayawayNotFoundException
                },
            )
        }
    }
}
