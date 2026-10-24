package com.shoetrack.domain.model

import kotlin.time.Instant

/** Read model for list screens: a layaway together with the names it refers to. */
data class LayawaySummary(
    val id: String,
    val customerName: String,
    val productDescription: String,
    val productPhotoPath: String,
    val remainingBalance: Money,
    val state: LayawayState,
    val lastPaymentAt: Instant,
)
