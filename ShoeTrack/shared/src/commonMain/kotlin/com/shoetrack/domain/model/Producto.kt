package com.shoetrack.domain.model

data class Producto(
    val id: String,
    val descripcion: String,
    val fotoPath: String,
    val precioTotal: Money
)
