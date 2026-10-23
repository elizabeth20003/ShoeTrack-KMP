package com.shoetrack.data.repository

import com.shoetrack.data.mapper.toDomain
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.model.Product
import com.shoetrack.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ProductRepositoryImpl(
    database: ShoeTrackDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ProductRepository {

    private val queries = database.productQueries

    override suspend fun save(product: Product) {
        withContext(dispatcher) {
            queries.upsertProduct(
                product.id,
                product.description,
                product.photoPath,
                product.totalPrice.cents,
            )
        }
    }

    override suspend fun getById(id: String): Product? = withContext(dispatcher) {
        queries.selectProductById(id).executeAsOneOrNull()?.toDomain()
    }
}
