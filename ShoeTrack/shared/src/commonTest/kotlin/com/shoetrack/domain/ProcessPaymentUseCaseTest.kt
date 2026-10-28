package com.shoetrack.domain

import com.shoetrack.domain.error.PaymentExceedsBalanceException
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Payment
import com.shoetrack.domain.usecase.ProcessPaymentUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class ProcessPaymentUseCaseTest {

    private val processPayment = ProcessPaymentUseCase()
    private val baseDate = Instant.parse("2026-01-01T00:00:00Z")

    private fun layaway(
        balance: Long,
        state: LayawayState = LayawayState.Active,
    ) = Layaway(
        id = "layaway-1",
        customerId = "customer-1",
        productId = "product-1",
        remainingBalance = Money(balance),
        state = state,
        createdAt = baseDate,
        lastPaymentAt = baseDate,
    )

    private fun payment(amount: Long) = Payment(
        id = "payment-1",
        layawayId = "layaway-1",
        amount = Money(amount),
        paidAt = baseDate,
        recordedByDeviceId = "device-1",
    )

    @Test
    fun partialPaymentReducesBalanceAndKeepsLayawayActive() {
        val result = processPayment(layaway(balance = 10_000), payment(3_000))

        assertEquals(7_000, result.remainingBalance.cents)
        assertEquals(LayawayState.Active, result.state)
    }

    @Test
    fun paymentCoveringExactBalanceCompletesLayaway() {
        val result = processPayment(layaway(balance = 5_000), payment(5_000))

        assertEquals(0, result.remainingBalance.cents)
        assertEquals(LayawayState.Completed, result.state)
    }

    @Test
    fun paymentAboveBalanceIsRejected() {
        assertFailsWith<PaymentExceedsBalanceException> {
            processPayment(layaway(balance = 3_000), payment(5_000))
        }
    }

    @Test
    fun paymentIsAddedToHistory() {
        val payment = payment(3_000)
        val result = processPayment(layaway(balance = 10_000), payment)

        assertEquals(listOf(payment), result.payments)
    }

    @Test
    fun completedLayawayRejectsPayments() {
        val completed = layaway(balance = 0, state = LayawayState.Completed)

        assertFailsWith<IllegalArgumentException> { processPayment(completed, payment(1_000)) }
    }

    @Test
    fun cancelledLayawayRejectsPayments() {
        val cancelled = layaway(balance = 5_000, state = LayawayState.Cancelled)

        assertFailsWith<IllegalArgumentException> { processPayment(cancelled, payment(1_000)) }
    }

    @Test
    fun overdueLayawayBecomesActiveAgainAfterPartialPayment() {
        val overdue = layaway(balance = 10_000, state = LayawayState.Overdue)
        val result = processPayment(overdue, payment(2_000))

        assertEquals(LayawayState.Active, result.state)
    }
}
