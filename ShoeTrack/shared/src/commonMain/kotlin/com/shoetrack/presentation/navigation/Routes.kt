package com.shoetrack.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object LayawayListRoute

/** A new layaway, optionally started from what was read of a notebook line. */
@Serializable
data class NewLayawayRoute(
    val fromNotebook: Boolean = false,
    val customerName: String = "",
    val productDescription: String = "",
    val priceUnits: Long? = null,
    val paidUnits: Long? = null,
    val lastPaymentEpochDay: Long? = null,
)

@Serializable
data object ScanRoute

@Serializable
data class LayawayDetailRoute(val id: String)
