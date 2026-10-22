package com.shoetrack.domain.repository

import com.shoetrack.domain.model.Producto

interface ProductoRepository {

    /** Inserta el producto o reemplaza el existente con el mismo id. */
    suspend fun guardar(producto: Producto)

    suspend fun obtenerPorId(id: String): Producto?
}
