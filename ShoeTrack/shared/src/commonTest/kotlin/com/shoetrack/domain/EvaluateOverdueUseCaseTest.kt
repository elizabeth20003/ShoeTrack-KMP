package com.shoetrack.domain

import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.OverduePolicy
import com.shoetrack.domain.usecase.EvaluateOverdueUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class EvaluateOverdueUseCaseTest {

    private val evaluateOverdue = EvaluateOverdueUseCase(OverduePolicy(overdueAfterDays = 30))
    private val lastPaymentAt = Instant.parse("2026-01-01T00:00:00Z")

    private fun layaway(state: LayawayState) = Layaway(
        id = "layaway-1",
        customerId = "customer-1",
        productId = "product-1",
        remainingBalance = Money(10_000),
        state = state,
        createdAt = lastPaymentAt,
        lastPaymentAt = lastPaymentAt,
    )

    @Test
    fun activeLayawayBelowTheLimitStaysActive() {
        val result = evaluateOverdue(layaway(LayawayState.Active), lastPaymentAt + 29.days)

        assertEquals(LayawayState.Active, result.state)
    }

    @Test
    fun activeLayawayReachingTheLimitBecomesOverdue() {
        val result = evaluateOverdue(layaway(LayawayState.Active), lastPaymentAt + 30.days)

        assertEquals(LayawayState.Overdue, result.state)
    }

    @Test
    fun completedLayawayNeverChanges() {
        val result = evaluateOverdue(layaway(LayawayState.Completed), lastPaymentAt + 365.days)

        assertEquals(LayawayState.Completed, result.state)
    }

    @Test
    fun cancelledLayawayNeverChanges() {
        val result = evaluateOverdue(layaway(LayawayState.Cancelled), lastPaymentAt + 365.days)

        assertEquals(LayawayState.Cancelled, result.state)
    }
}
