package com.shoetrack.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.shoetrack.data.mapper.toDbValue
import com.shoetrack.data.mapper.toDomain
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.model.Abono
import com.shoetrack.domain.model.Apartado
import com.shoetrack.domain.model.ApartadoState
import com.shoetrack.domain.repository.ApartadoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * Implementación SQLDelight de [ApartadoRepository].
 *
 * Cada escritura marca el registro como pendiente de sincronizar y guarda qué
 * dispositivo ([deviceId]) y cuándo lo modificó; con eso el motor de sincronización
 * podrá resolver cambios hechos offline desde varios dispositivos.
 */
internal class ApartadoRepositoryImpl(
    private val database: ShoeTrackDatabase,
    private val deviceId: String,
    private val clock: Clock = Clock.System,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ApartadoRepository {

    private val apartadoQueries = database.apartadoQueries
    private val abonoQueries = database.abonoQueries

    override suspend fun crear(apartado: Apartado) {
        withContext(dispatcher) {
            apartadoQueries.insertApartado(
                apartado.id,
                apartado.clienteId,
                apartado.productoId,
                apartado.saldoPendiente.centavos,
                apartado.estado.toDbValue(),
                apartado.fechaCreacion.toEpochMilliseconds(),
                apartado.fechaUltimoAbono.toEpochMilliseconds(),
                clock.now().toEpochMilliseconds(),
                deviceId,
                PENDIENTE_DE_SYNC,
            )
        }
    }

    override suspend fun registrarAbono(apartado: Apartado, abono: Abono) {
        require(abono.apartadoId == apartado.id) {
            "El abono ${abono.id} pertenece al apartado ${abono.apartadoId}, no a ${apartado.id}"
        }
        withContext(dispatcher) {
            database.transaction {
                abonoQueries.insertAbono(
                    abono.id,
                    abono.apartadoId,
                    abono.monto.centavos,
                    abono.fecha.toEpochMilliseconds(),
                    abono.registradoPorDeviceId,
                )
                actualizarFila(apartado)
            }
        }
    }

    override suspend fun actualizarEstado(apartado: Apartado) {
        withContext(dispatcher) { actualizarFila(apartado) }
    }

    override suspend fun obtenerPorId(id: String): Apartado? = withContext(dispatcher) {
        val fila = apartadoQueries.selectApartadoById(id).executeAsOneOrNull()
            ?: return@withContext null
        val abonos = abonoQueries.selectAbonosPorApartado(id).executeAsList().map { it.toDomain() }
        fila.toDomain(abonos)
    }

    override suspend fun obtenerPorEstado(estado: ApartadoState): List<Apartado> =
        withContext(dispatcher) {
            apartadoQueries.selectApartadosPorEstado(estado.toDbValue())
                .executeAsList()
                .map { it.toDomain() }
        }

    override fun observarTodos(): Flow<List<Apartado>> =
        apartadoQueries.selectAllApartados()
            .asFlow()
            .mapToList(dispatcher)
            .map { filas -> filas.map { it.toDomain() } }

    override suspend fun obtenerPendientesDeSync(): List<Apartado> = withContext(dispatcher) {
        apartadoQueries.selectPendientesDeSync().executeAsList().map { it.toDomain() }
    }

    private fun actualizarFila(apartado: Apartado) {
        apartadoQueries.updateApartado(
            apartado.saldoPendiente.centavos,
            apartado.estado.toDbValue(),
            apartado.fechaUltimoAbono.toEpochMilliseconds(),
            clock.now().toEpochMilliseconds(),
            deviceId,
            apartado.id,
        )
    }

    private companion object {
        const val PENDIENTE_DE_SYNC = 1L
    }
}
