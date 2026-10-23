package com.shoetrack.domain.model

import kotlin.time.Instant

data class Customer(
    val id: String,
    val name: String,
    val phone: String,
    val registeredAt: Instant,
)
