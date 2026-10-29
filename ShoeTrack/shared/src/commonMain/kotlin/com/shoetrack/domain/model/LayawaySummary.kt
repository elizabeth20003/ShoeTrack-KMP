package com.shoetrack.domain.model

import kotlin.time.Instant

/** Read model for list screens: a layaway together with the names it refers to. */
data class LayawaySummary(
    val id: String,
    val customerName: String,
    val productDescription: String,
    val productPhotoPath: String,
    val remainingBalance: Money,
    val totalPrice: Money,
    val state: LayawayState,
    val lastPaymentAt: Instant,
) {
    /** How much of the price is paid, from 0 to 1. */
    val paidFraction: Float
        get() = if (totalPrice.isPositive) {
            (1f - remainingBalance.cents.toFloat() / totalPrice.cents).coerceIn(0f, 1f)
        } else {
            0f
        }
}
