package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.Abono
import com.shoetrack.domain.model.Apartado
import com.shoetrack.domain.model.ApartadoState

/**
 * Aplica un abono a un apartado, recalcula el saldo y determina
 * si el apartado pasa a estado Completado.
 */
class ProcesarAbonoUseCase {

    fun ejecutar(apartado: Apartado, abono: Abono): Apartado {
        require(apartado.estado !is ApartadoState.Cancelado) {
            "No se puede abonar a un apartado cancelado"
        }
        require(apartado.estado !is ApartadoState.Completado) {
            "El apartado ya está completado"
        }

        val nuevoSaldo = apartado.saldoPendiente - abono.monto
        val nuevoEstado = if (nuevoSaldo.esCeroOMenos) {
            ApartadoState.Completado
        } else {
            ApartadoState.Activo
        }

        return apartado.copy(
            saldoPendiente = nuevoSaldo,
            estado = nuevoEstado,
            fechaUltimoAbono = abono.fecha,
            abonos = apartado.abonos + abono
        )
    }
}
