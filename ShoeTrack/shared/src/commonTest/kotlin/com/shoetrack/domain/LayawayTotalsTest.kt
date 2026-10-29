package com.shoetrack.domain

import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.LayawaySummary
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.totals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class LayawayTotalsTest {

    private fun summary(balance: Long, state: LayawayState, total: Long = 100_000) = LayawaySummary(
        id = "id",
        customerName = "Ana Perez",
        productDescription = "Nike",
        productPhotoPath = "",
        remainingBalance = Money(balance),
        totalPrice = Money(total),
        state = state,
        lastPaymentAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    @Test
    fun addsUpOnlyTheOpenLayaways() {
        val totals = listOf(
            summary(30_000, LayawayState.Active),
            summary(20_000, LayawayState.Overdue),
            summary(0, LayawayState.Completed),
            summary(50_000, LayawayState.Cancelled),
        ).totals()

        assertEquals(Money(50_000), totals.owed)
        assertEquals(2, totals.openCount)
        assertEquals(1, totals.overdueCount)
    }

    @Test
    fun anEmptyListOwesNothing() {
        assertEquals(Money.ZERO, emptyList<LayawaySummary>().totals().owed)
    }

    @Test
    fun paidFractionIsHowMuchOfThePriceIsPaid() {
        assertEquals(0.7f, summary(30_000, LayawayState.Active).paidFraction, 0.001f)
        assertEquals(1f, summary(0, LayawayState.Completed).paidFraction, 0.001f)
    }
}
