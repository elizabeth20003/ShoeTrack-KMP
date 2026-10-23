package com.shoetrack.domain.model

import kotlin.time.Instant

/** A partial payment. [recordedByDeviceId] tells which device recorded it, for sync. */
data class Payment(
    val id: String,
    val layawayId: String,
    val amount: Money,
    val paidAt: Instant,
    val recordedByDeviceId: String,
)
