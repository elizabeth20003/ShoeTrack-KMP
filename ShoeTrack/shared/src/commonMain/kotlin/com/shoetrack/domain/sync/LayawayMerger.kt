package com.shoetrack.domain.sync

import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Payment

/**
 * Combines two versions of the same layaway so every device ends with the same result,
 * whatever the order in which they exchange changes.
 *
 * Payments are never edited, so the merged history is the union of both sets, and the
 * balance and last payment date are derived from it instead of being picked from one side.
 * A cancellation is final. Becoming overdue depends on the clock, so it is not decided here.
 */
class LayawayMerger {

    fun merge(local: Layaway?, remote: Layaway, totalPrice: Money): Layaway {
        val payments = unionOfPayments(local, remote)
        val paid = payments.fold(Money.ZERO) { total, payment -> total + payment.amount }
        val remaining = totalPrice - paid

        return remote.copy(
            remainingBalance = remaining,
            state = mergedState(local, remote, remaining),
            lastPaymentAt = payments.maxOfOrNull { it.paidAt } ?: remote.createdAt,
            payments = payments,
        )
    }

    private fun unionOfPayments(local: Layaway?, remote: Layaway): List<Payment> =
        ((local?.payments.orEmpty()) + remote.payments)
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.paidAt }, { it.id }))

    private fun mergedState(local: Layaway?, remote: Layaway, remaining: Money): LayawayState = when {
        local?.state == LayawayState.Cancelled || remote.state == LayawayState.Cancelled ->
            LayawayState.Cancelled
        remaining.isZeroOrLess -> LayawayState.Completed
        else -> LayawayState.Active
    }
}
