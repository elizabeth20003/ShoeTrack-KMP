package com.shoetrack.domain.model

import kotlin.time.Instant

/**
 * registradoPorDeviceId identifica qué dispositivo/vendedor registró
 * este abono. Es la pieza clave para resolver sincronización cuando
 * varios vendedores trabajan offline y luego se reconectan.
 */
data class Abono(
    val id: String,
    val apartadoId: String,
    val monto: Money,
    val fecha: Instant,
    val registradoPorDeviceId: String
)
