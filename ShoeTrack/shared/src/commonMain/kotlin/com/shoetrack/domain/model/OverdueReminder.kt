package com.shoetrack.domain.model

import kotlin.time.Instant

/** A notification to show at [dueAt] if the layaway still has not received a payment. */
data class OverdueReminder(
    val layawayId: String,
    val dueAt: Instant,
    val customerName: String,
    val productDescription: String,
    val remainingBalance: Money,
)
