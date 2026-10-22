package com.shoetrack.domain.repository

import com.shoetrack.domain.model.Abono
import com.shoetrack.domain.model.Apartado
import com.shoetrack.domain.model.ApartadoState
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de persistencia de apartados.
 *
 * Los apartados devueltos por [observarTodos] y [obtenerPorEstado] NO incluyen
 * su historial de abonos (`abonos` viene vacío) para mantener las listas ligeras.
 * Usa [obtenerPorId] cuando necesites el historial completo.
 */
interface ApartadoRepository {

    suspend fun crear(apartado: Apartado)

    /**
     * Guarda el [abono] y el [apartado] ya actualizado (saldo, estado y fecha del
     * último abono) en una sola transacción: o se guardan los dos o ninguno.
     */
    suspend fun registrarAbono(apartado: Apartado, abono: Abono)

    /** Persiste un cambio de estado que no viene de un abono (p. ej. paso a mora). */
    suspend fun actualizarEstado(apartado: Apartado)

    /** Devuelve el apartado con su historial completo de abonos, o null si no existe. */
    suspend fun obtenerPorId(id: String): Apartado?

    suspend fun obtenerPorEstado(estado: ApartadoState): List<Apartado>

    /** Emite todos los apartados (el último abono primero) cada vez que algo cambia. */
    fun observarTodos(): Flow<List<Apartado>>

    /** Apartados con cambios locales aún no sincronizados con otros dispositivos. */
    suspend fun obtenerPendientesDeSync(): List<Apartado>
}
