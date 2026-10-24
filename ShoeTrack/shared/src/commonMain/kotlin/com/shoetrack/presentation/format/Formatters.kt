package com.shoetrack.presentation.format

import com.shoetrack.domain.model.Money
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

const val MAX_MONEY_DIGITS = 9

fun Money.format(): String = "$ ${(cents / 100).withThousandsSeparator()}"

fun Instant.formatAsDate(): String =
    toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

/** Parses what the user typed as whole currency units, ignoring any non-digit character. */
fun String.toMoneyOrNull(): Money? =
    filter(Char::isDigit).take(MAX_MONEY_DIGITS).toLongOrNull()?.let(Money::fromUnits)

private fun Long.withThousandsSeparator(): String =
    toString().reversed().chunked(3).joinToString(".").reversed()
