package com.shoetrack.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object LayawayListRoute

@Serializable
data object NewLayawayRoute

@Serializable
data class LayawayDetailRoute(val id: String)
