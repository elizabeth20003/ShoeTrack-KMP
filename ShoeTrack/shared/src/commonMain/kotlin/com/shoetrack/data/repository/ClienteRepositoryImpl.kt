package com.shoetrack.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.shoetrack.data.mapper.toDomain
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.model.Cliente
import com.shoetrack.domain.repository.ClienteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class ClienteRepositoryImpl(
    database: ShoeTrackDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ClienteRepository {

    private val queries = database.clienteQueries

    override suspend fun guardar(cliente: Cliente) {
        withContext(dispatcher) {
            queries.upsertCliente(
                cliente.id,
                cliente.nombre,
                cliente.telefono,
                cliente.fechaRegistro.toEpochMilliseconds(),
            )
        }
    }

    override suspend fun obtenerPorId(id: String): Cliente? = withContext(dispatcher) {
        queries.selectClienteById(id).executeAsOneOrNull()?.toDomain()
    }

    override fun observarTodos(): Flow<List<Cliente>> =
        queries.selectAllClientes()
            .asFlow()
            .mapToList(dispatcher)
            .map { filas -> filas.map { it.toDomain() } }
}
