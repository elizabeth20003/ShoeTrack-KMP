package com.shoetrack.domain.model

import kotlin.time.Instant

sealed class ApartadoState {
    data object Activo : ApartadoState()
    data object EnMora : ApartadoState()
    data object Completado : ApartadoState()
    data object Cancelado : ApartadoState()
}

data class Apartado(
    val id: String,
    val clienteId: String,
    val productoId: String,
    val saldoPendiente: Money,
    val estado: ApartadoState,
    val fechaCreacion: Instant,
    val fechaUltimoAbono: Instant,
    val abonos: List<Abono> = emptyList()
)
