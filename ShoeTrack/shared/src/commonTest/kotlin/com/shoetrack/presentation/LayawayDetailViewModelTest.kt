package com.shoetrack.presentation

import com.shoetrack.domain.fakes.FakeCustomerRepository
import com.shoetrack.domain.fakes.FakeLayawayRepository
import com.shoetrack.domain.fakes.FakeProductRepository
import com.shoetrack.domain.fakes.FixedClock
import com.shoetrack.domain.fakes.SequentialIdGenerator
import com.shoetrack.domain.fakes.scheduleReminderOf
import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Product
import com.shoetrack.domain.usecase.GetLayawayDetailUseCase
import com.shoetrack.domain.usecase.ProcessPaymentUseCase
import com.shoetrack.domain.usecase.RegisterPaymentUseCase
import com.shoetrack.presentation.detail.LayawayDetailEvent
import com.shoetrack.presentation.detail.LayawayDetailViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class LayawayDetailViewModelTest : MainDispatcherTest() {

    private val now = Instant.parse("2026-02-01T00:00:00Z")
    private val layaways = FakeLayawayRepository()
    private val customers = FakeCustomerRepository()
    private val products = FakeProductRepository()

    private fun storeLayaway() {
        customers.customers["customer-1"] = Customer("customer-1", "Ana Perez", "3001234567", now)
        products.products["product-1"] = Product("product-1", "Nike Air Force 42", "", Money.fromUnits(100_000))
        layaways.layaways["layaway-1"] = Layaway(
            id = "layaway-1",
            customerId = "customer-1",
            productId = "product-1",
            remainingBalance = Money.fromUnits(100_000),
            state = LayawayState.Active,
            createdAt = now,
            lastPaymentAt = now,
        )
    }

    private fun viewModel(layawayId: String = "layaway-1") = LayawayDetailViewModel(
        layawayId = layawayId,
        getLayawayDetail = GetLayawayDetailUseCase(layaways, customers, products),
        registerPayment = RegisterPaymentUseCase(
            layawayRepository = layaways,
            processPayment = ProcessPaymentUseCase(),
            idGenerator = SequentialIdGenerator(),
            clock = FixedClock(now),
            deviceId = "device-1",
            scheduleReminder = scheduleReminderOf(layaways, customers, products),
        ),
    )

    @Test
    fun loadsTheLayawayDetailOnCreation() {
        storeLayaway()

        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.detail)
    }

    @Test
    fun unknownLayawayEndsLoadingWithoutDetailOrError() {
        val state = viewModel("missing").uiState.value

        assertFalse(state.isLoading)
        assertNull(state.detail)
        assertFalse(state.loadFailed)
    }

    @Test
    fun confirmingAPaymentReducesTheBalanceAndClosesTheDialog() {
        storeLayaway()
        val viewModel = viewModel()
        viewModel.openPaymentDialog()
        viewModel.onPaymentInputChange("40000")

        viewModel.confirmPayment()

        val state = viewModel.uiState.value
        assertEquals(Money.fromUnits(60_000), state.detail?.layaway?.remainingBalance)
        assertFalse(state.isPaymentDialogOpen)
        assertEquals("", state.paymentInput)
    }

    @Test
    fun aFailedPaymentEmitsAnEventAndKeepsTheBalance() = runTest {
        storeLayaway()
        val viewModel = viewModel()
        layaways.writeFailure = IllegalStateException("disk full")
        viewModel.onPaymentInputChange("40000")

        viewModel.confirmPayment()

        assertEquals(LayawayDetailEvent.PaymentFailed, viewModel.events.first())
        assertEquals(Money.fromUnits(100_000), viewModel.uiState.value.detail?.layaway?.remainingBalance)
    }

    @Test
    fun emptyPaymentInputCannotBeConfirmed() {
        storeLayaway()
        val viewModel = viewModel()

        assertFalse(viewModel.uiState.value.canConfirmPayment)
        viewModel.onPaymentInputChange("500")
        assertTrue(viewModel.uiState.value.canConfirmPayment)
    }

    @Test
    fun aPaymentAboveTheBalanceCannotBeConfirmed() {
        storeLayaway()
        val viewModel = viewModel()

        viewModel.onPaymentInputChange("100001")

        assertTrue(viewModel.uiState.value.isPaymentTooHigh)
        assertFalse(viewModel.uiState.value.canConfirmPayment)
    }

    @Test
    fun aPaymentEqualToTheBalanceCanBeConfirmed() {
        storeLayaway()
        val viewModel = viewModel()

        viewModel.onPaymentInputChange("100000")

        assertTrue(viewModel.uiState.value.canConfirmPayment)
    }
}
