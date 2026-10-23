package com.shoetrack.domain.model

import kotlin.time.Instant

sealed class LayawayState {
    data object Active : LayawayState()
    data object Overdue : LayawayState()
    data object Completed : LayawayState()
    data object Cancelled : LayawayState()
}

data class Layaway(
    val id: String,
    val customerId: String,
    val productId: String,
    val remainingBalance: Money,
    val state: LayawayState,
    val createdAt: Instant,
    val lastPaymentAt: Instant,
    val payments: List<Payment> = emptyList(),
)
