package com.shoetrack.domain.model

/** What is still owed to the store across the layaways that are open. */
data class LayawayTotals(
    val owed: Money,
    val openCount: Int,
    val overdueCount: Int,
)

fun List<LayawaySummary>.totals(): LayawayTotals {
    val open = filter { it.state == LayawayState.Active || it.state == LayawayState.Overdue }
    return LayawayTotals(
        owed = open.fold(Money.ZERO) { total, layaway -> total + layaway.remainingBalance },
        openCount = open.size,
        overdueCount = open.count { it.state == LayawayState.Overdue },
    )
}
