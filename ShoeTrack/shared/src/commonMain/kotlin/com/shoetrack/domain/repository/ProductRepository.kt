package com.shoetrack.domain.repository

import com.shoetrack.domain.model.Product

interface ProductRepository {

    suspend fun save(product: Product)

    suspend fun getById(id: String): Product?
}
