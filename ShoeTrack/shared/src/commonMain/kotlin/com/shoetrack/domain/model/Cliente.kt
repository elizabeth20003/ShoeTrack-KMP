package com.shoetrack.domain.model

import kotlin.time.Instant

data class Cliente(
    val id: String,
    val nombre: String,
    val telefono: String,
    val fechaRegistro: Instant
)
