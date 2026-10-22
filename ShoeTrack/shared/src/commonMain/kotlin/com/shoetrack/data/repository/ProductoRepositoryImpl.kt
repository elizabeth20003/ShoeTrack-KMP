package com.shoetrack.data.repository

import com.shoetrack.data.mapper.toDomain
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.model.Producto
import com.shoetrack.domain.repository.ProductoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ProductoRepositoryImpl(
    database: ShoeTrackDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ProductoRepository {

    private val queries = database.productoQueries

    override suspend fun guardar(producto: Producto) {
        withContext(dispatcher) {
            queries.upsertProducto(
                producto.id,
                producto.descripcion,
                producto.fotoPath,
                producto.precioTotal.centavos,
            )
        }
    }

    override suspend fun obtenerPorId(id: String): Producto? = withContext(dispatcher) {
        queries.selectProductoById(id).executeAsOneOrNull()?.toDomain()
    }
}
