package com.shoetrack.domain.model

import kotlin.jvm.JvmInline

/**
 * Representa dinero en centavos para evitar errores de redondeo
 * que ocurrirían usando Double/Float para valores monetarios.
 */
@JvmInline
value class Money(val centavos: Long) {

    operator fun minus(otro: Money): Money = Money(centavos - otro.centavos)
    operator fun plus(otro: Money): Money = Money(centavos + otro.centavos)
    operator fun compareTo(otro: Money): Int = centavos.compareTo(otro.centavos)

    val esPositivo: Boolean get() = centavos > 0
    val esCeroOMenos: Boolean get() = centavos <= 0

    companion object {
        fun pesos(pesos: Long): Money = Money(pesos * 100)
        val CERO = Money(0)
    }
}
