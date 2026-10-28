package com.shoetrack.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.usecase.NewLayaway
import com.shoetrack.domain.usecase.SavePhotoUseCase
import com.shoetrack.domain.usecase.SearchCustomersUseCase
import com.shoetrack.domain.usecase.StartLayawayUseCase
import com.shoetrack.domain.validation.PhoneNumberRules
import com.shoetrack.presentation.common.runCatchingExceptCancellation
import com.shoetrack.presentation.format.MAX_MONEY_DIGITS
import com.shoetrack.presentation.format.toMoneyOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewLayawayUiState(
    val customerName: String = "",
    val customerPhone: String = "",
    val productDescription: String = "",
    val price: String = "",
    val initialPayment: String = "",
    val photoName: String? = null,
    val customerSuggestions: List<Customer> = emptyList(),
    val selectedCustomerId: String? = null,
    val isSaving: Boolean = false,
    val showValidation: Boolean = false,
) {
    val isCustomerNameInvalid: Boolean get() = showValidation && customerName.isBlank()
    val isCustomerPhoneInvalid: Boolean get() = showValidation && !PhoneNumberRules.isValid(customerPhone)
    val isProductDescriptionInvalid: Boolean get() = showValidation && productDescription.isBlank()
    val isPriceInvalid: Boolean get() = showValidation && price.toMoneyOrNull()?.isPositive != true

    val isInitialPaymentInvalid: Boolean
        get() {
            val initial = initialPayment.toMoneyOrNull() ?: return false
            val total = price.toMoneyOrNull() ?: return false
            return initial > total
        }

    val isValid: Boolean
        get() = !isInitialPaymentInvalid &&
            customerName.isNotBlank() &&
            productDescription.isNotBlank() &&
            PhoneNumberRules.isValid(customerPhone) &&
            price.toMoneyOrNull()?.isPositive == true
}

/** One-time outcomes the screen reacts to, kept apart from the persistent state. */
sealed interface NewLayawayEvent {
    data object Saved : NewLayawayEvent
    data object SaveFailed : NewLayawayEvent
    data object PhotoFailed : NewLayawayEvent
}

class NewLayawayViewModel(
    private val startLayaway: StartLayawayUseCase,
    private val savePhoto: SavePhotoUseCase,
    private val searchCustomers: SearchCustomersUseCase,
) : ViewModel() {

    private var suggestionsJob: Job? = null

    private val _uiState = MutableStateFlow(NewLayawayUiState())
    val uiState: StateFlow<NewLayawayUiState> = _uiState.asStateFlow()

    private val _events = Channel<NewLayawayEvent>(Channel.BUFFERED)
    val events: Flow<NewLayawayEvent> = _events.receiveAsFlow()

    fun onCustomerNameChange(value: String) {
        _uiState.update { it.copy(customerName = value, selectedCustomerId = null) }
        suggestionsJob?.cancel()
        suggestionsJob = viewModelScope.launch {
            val suggestions = runCatchingExceptCancellation { searchCustomers(value) }.getOrDefault(emptyList())
            _uiState.update { it.copy(customerSuggestions = suggestions) }
        }
    }

    fun onCustomerPhoneChange(value: String) =
        _uiState.update {
            it.copy(customerPhone = PhoneNumberRules.normalize(value).take(MAX_PHONE_DIGITS), selectedCustomerId = null)
        }

    fun onCustomerSelected(customer: Customer) {
        suggestionsJob?.cancel()
        _uiState.update {
            it.copy(
                customerName = customer.name,
                customerPhone = customer.phone,
                selectedCustomerId = customer.id,
                customerSuggestions = emptyList(),
            )
        }
    }

    fun onProductDescriptionChange(value: String) =
        _uiState.update { it.copy(productDescription = value) }

    fun onPriceChange(value: String) = _uiState.update { it.copy(price = value.digitsOnly()) }

    fun onInitialPaymentChange(value: String) =
        _uiState.update { it.copy(initialPayment = value.digitsOnly()) }

    fun onPhotoPicked(imageBytes: ByteArray) {
        viewModelScope.launch {
            runCatchingExceptCancellation { savePhoto(imageBytes) }
                .onSuccess { name -> _uiState.update { it.copy(photoName = name) } }
                .onFailure { _events.send(NewLayawayEvent.PhotoFailed) }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val price = state.price.toMoneyOrNull()
        if (!state.isValid || price == null) {
            _uiState.update { it.copy(showValidation = true) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = runCatchingExceptCancellation {
                startLayaway(
                    NewLayaway(
                        customerName = state.customerName,
                        customerPhone = state.customerPhone,
                        productDescription = state.productDescription,
                        productPhotoPath = state.photoName.orEmpty(),
                        productPrice = price,
                        initialPayment = state.initialPayment.toMoneyOrNull() ?: Money.ZERO,
                        existingCustomerId = state.selectedCustomerId,
                    ),
                )
            }
            _uiState.update { it.copy(isSaving = false) }
            _events.send(if (result.isSuccess) NewLayawayEvent.Saved else NewLayawayEvent.SaveFailed)
        }
    }

    private companion object {
        const val MAX_PHONE_DIGITS = 10
    }

    private fun String.digitsOnly(): String = filter(Char::isDigit).take(MAX_MONEY_DIGITS)
}
