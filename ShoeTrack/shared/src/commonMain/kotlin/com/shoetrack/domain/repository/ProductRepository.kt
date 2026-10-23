package com.shoetrack.domain.repository

import com.shoetrack.domain.model.Product

interface ProductRepository {

    /** Inserts the product, or replaces the one with the same id. */
    suspend fun save(product: Product)

    suspend fun getById(id: String): Product?
}
