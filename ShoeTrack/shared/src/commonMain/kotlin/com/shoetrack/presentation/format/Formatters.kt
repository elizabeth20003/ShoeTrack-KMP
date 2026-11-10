package com.shoetrack.presentation.format

import com.shoetrack.domain.model.Money
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Instant

const val MAX_MONEY_DIGITS = 9

fun Money.format(): String = "$ ${(cents / 100).withThousandsSeparator()}"

/** A fraction such as 0.6 as "60%". */
fun Float.formatAsPercent(): String = "${(this * 100).roundToInt()}%"

fun Instant.formatAsDate(): String = toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

fun Instant.formatAsTime(): String {
    val time = toLocalDateTime(TimeZone.currentSystemDefault()).time
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}

/** Parses what the user typed as whole currency units, ignoring any non-digit character. */
fun String.toMoneyOrNull(): Money? = filter(Char::isDigit).take(MAX_MONEY_DIGITS).toLongOrNull()?.let(Money::fromUnits)

private fun Long.withThousandsSeparator(): String = toString()
    .reversed()
    .chunked(3)
    .joinToString(".")
    .reversed()

/** The date picker works in UTC days; this turns its pick into the start of that day on this device. */
fun Long.pickedDateToInstant(): Instant {
    val day = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
    return day.atStartOfDayIn(TimeZone.currentSystemDefault())
}

/** The inverse of [pickedDateToInstant], used to show the current choice in the date picker. */
fun Instant.toPickerMillis(): Long {
    val day = toLocalDateTime(TimeZone.currentSystemDefault()).date
    return day.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
}

/** True when the picker's UTC day is not after today on this device. */
fun isPickableDay(utcMillis: Long, now: Instant): Boolean = utcMillis <= now.toPickerMillis()
