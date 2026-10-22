package com.shoetrack.domain.repository

import com.shoetrack.domain.model.Cliente
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acceso a clientes. El dominio solo conoce esta interfaz;
 * la implementación concreta (SQLDelight) vive en la capa de datos.
 */
interface ClienteRepository {

    /** Inserta el cliente o reemplaza el existente con el mismo id. */
    suspend fun guardar(cliente: Cliente)

    suspend fun obtenerPorId(id: String): Cliente?

    /** Emite la lista completa (ordenada por nombre) cada vez que cambia. */
    fun observarTodos(): Flow<List<Cliente>>
}
