package com.shoetrack.domain.model

import kotlin.jvm.JvmInline

/** Money as a whole number of cents, avoiding floating-point rounding errors. */
@JvmInline
value class Money(val cents: Long) {

    operator fun plus(other: Money): Money = Money(cents + other.cents)
    operator fun minus(other: Money): Money = Money(cents - other.cents)
    operator fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    val isPositive: Boolean get() = cents > 0
    val isZeroOrLess: Boolean get() = cents <= 0

    companion object {
        val ZERO = Money(0)

        fun fromUnits(units: Long): Money = Money(units * 100)
    }
}
