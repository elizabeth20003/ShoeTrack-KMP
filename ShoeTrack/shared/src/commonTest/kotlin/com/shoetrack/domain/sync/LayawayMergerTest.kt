package com.shoetrack.domain.sync

import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Payment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class LayawayMergerTest {

    private val merger = LayawayMerger()
    private val start = Instant.parse("2026-01-01T00:00:00Z")
    private val price = Money(10_000)

    private fun layaway(
        vararg payments: Payment,
        state: LayawayState = LayawayState.Active,
    ): Layaway {
        val paid = payments.fold(Money.ZERO) { total, payment -> total + payment.amount }
        return Layaway(
            id = "layaway-1",
            customerId = "customer-1",
            productId = "product-1",
            remainingBalance = price - paid,
            state = state,
            createdAt = start,
            lastPaymentAt = payments.maxOfOrNull { it.paidAt } ?: start,
            payments = payments.toList(),
        )
    }

    private fun payment(id: String, amount: Long, day: Int, device: String = "device-a") =
        Payment(id, "layaway-1", Money(amount), start + day.days, device)

    @Test
    fun keepsThePaymentsRecordedOnBothDevices() {
        val onA = layaway(payment("p1", 2_000, 1, "device-a"))
        val onB = layaway(payment("p2", 3_000, 2, "device-b"))

        val merged = merger.merge(onA, onB, price)

        assertEquals(listOf("p1", "p2"), merged.payments.map { it.id })
        assertEquals(Money(5_000), merged.remainingBalance)
        assertEquals(start + 2.days, merged.lastPaymentAt)
    }

    @Test
    fun aPaymentKnownToBothSidesIsCountedOnce() {
        val shared = payment("p1", 2_000, 1)

        val merged = merger.merge(layaway(shared), layaway(shared), price)

        assertEquals(Money(8_000), merged.remainingBalance)
        assertEquals(1, merged.payments.size)
    }

    @Test
    fun becomesCompletedWhenTheCombinedPaymentsCoverThePrice() {
        val onA = layaway(payment("p1", 6_000, 1))
        val onB = layaway(payment("p2", 4_000, 2, "device-b"))

        assertEquals(LayawayState.Completed, merger.merge(onA, onB, price).state)
    }

    @Test
    fun aCancellationOnEitherSideWins() {
        val active = layaway(payment("p1", 2_000, 1))
        val cancelled = layaway(state = LayawayState.Cancelled)

        assertEquals(LayawayState.Cancelled, merger.merge(active, cancelled, price).state)
        assertEquals(LayawayState.Cancelled, merger.merge(cancelled, active, price).state)
    }

    @Test
    fun aLayawayNewToThisDeviceIsTakenWithItsPayments() {
        val remote = layaway(payment("p1", 2_000, 1))

        val merged = merger.merge(local = null, remote = remote, totalPrice = price)

        assertEquals(remote, merged)
    }

    @Test
    fun mergingInEitherOrderGivesTheSameResult() {
        val onA = layaway(payment("p1", 2_000, 1), payment("p3", 1_000, 3))
        val onB = layaway(payment("p2", 3_000, 2, "device-b"))

        assertEquals(merger.merge(onA, onB, price), merger.merge(onB, onA, price))
    }

    @Test
    fun mergingTheSameDataAgainChangesNothing() {
        val onA = layaway(payment("p1", 2_000, 1))
        val onB = layaway(payment("p2", 3_000, 2, "device-b"))
        val once = merger.merge(onA, onB, price)

        assertEquals(once, merger.merge(once, onB, price))
    }
}
