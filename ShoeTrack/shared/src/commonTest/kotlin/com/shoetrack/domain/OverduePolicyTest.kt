package com.shoetrack.domain

import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.OverduePolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class OverduePolicyTest {

    private val policy = OverduePolicy(overdueAfterDays = 30)
    private val lastPaymentAt = Instant.parse("2026-01-01T00:00:00Z")
    private val layaway = Layaway(
        id = "layaway-1",
        customerId = "customer-1",
        productId = "product-1",
        remainingBalance = Money(10_000),
        state = LayawayState.Active,
        createdAt = lastPaymentAt,
        lastPaymentAt = lastPaymentAt,
    )

    @Test
    fun dueDateIsTheLimitAfterTheLastPayment() {
        assertEquals(lastPaymentAt + 30.days, policy.dueAt(layaway))
    }

    @Test
    fun notOverdueBeforeTheDueDate() {
        assertFalse(policy.isOverdue(layaway, lastPaymentAt + 29.days))
    }

    @Test
    fun overdueFromTheDueDateOn() {
        assertTrue(policy.isOverdue(layaway, lastPaymentAt + 30.days))
    }
}
