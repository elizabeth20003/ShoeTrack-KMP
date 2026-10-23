package com.shoetrack.domain.model

data class Product(
    val id: String,
    val description: String,
    val photoPath: String,
    val totalPrice: Money,
)
