package com.shoetrack.domain.notebook

import com.shoetrack.domain.model.Money
import kotlinx.datetime.LocalDate

/**
 * Turns the text read from a photo of the notebook into a [NotebookEntry].
 *
 * The notebook has no columns: each layaway is a free-form line such as
 * `03/10/26 Alba Calambas Maxten gris hombre T37 140.000 Abona 30.000 Saldo 110.000`.
 * Text recognition confuses look-alike characters (0 and O, 1 and l, 5 and s), so the parser
 * accepts those in amounts and in the words that label them.
 */
object NotebookParser {

    private const val LETTERS = "A-Za-zÁÉÍÓÚÜáéíóúüÑñ"
    private const val DIGIT_LIKE = "0-9OoDlIi|SsBZ"
    private const val MAX_NAME_WORDS = 2
    private const val MIN_AMOUNT = 1_000L
    private const val MAX_LABEL_DISTANCE = 2
    private const val MIN_LONG_LABEL = 5

    // A line begins with a date, however badly it was read, then a word.
    private val datedLine = Regex("""^([\dOolI|?]{1,2}[\s/\-.]*[\dOolI|?]{1,3}[\s/\-.]*\d{2,4})\s+([$LETTERS].*)$""")
    private val date = Regex("""^(\d{1,2})[\s/\-.]*(\d{2,3})[\s/\-.]*(\d{2,4})$""")
    private val labelWithColon = Regex("""^([$LETTERS]{3,}):(.*)$""")
    private val thousandsHead = Regex("""^[$DIGIT_LIKE]{1,3}$""")
    private val thousandsTail = Regex("""^([0Oo]{3}|[05]00)$""")
    private val separatedAmount = Regex("""^([$DIGIT_LIKE]{1,3})[.,\-]([$DIGIT_LIKE]{3})$""")
    private val plainAmount = Regex("""^[$DIGIT_LIKE]{5,7}$""")
    private val word = Regex("""^[$LETTERS]{2,}$""")

    private enum class Label { Total, Paid, Balance }

    private class Labeled(val label: Label, val amount: Money?)

    fun parse(text: String): NotebookEntry {
        val lines = text.lines().map(String::trim).filter(String::isNotEmpty)
        val startIndex = lines.indexOfLast { startOf(it) != null }
        if (startIndex < 0) return parseWithoutDate(lines)

        val start = checkNotNull(startOf(lines[startIndex]))
        val following = lines.drop(startIndex + 1)
        val tokens = (listOf(start.groupValues[2]) + following).joinToString(" ").toTokens()
        // Each later payment is written on its own dated line; the newest date is the last payment.
        val lastPaymentDate = (listOf(lines[startIndex]) + following)
            .mapNotNull { datedLine.matchEntire(it)?.groupValues?.get(1) }
            .mapNotNull(::parseDate)
            .lastOrNull()
        return read(tokens, lastPaymentDate)
    }

    /** A badly read date must not discard the line: start at the first two words in a row, the customer. */
    private fun parseWithoutDate(lines: List<String>): NotebookEntry {
        val index = lines.indexOfFirst { line ->
            line.toTokens().windowed(2).any { (first, second) ->
                word.matches(first) && word.matches(second) && labelOf(first) == null
            }
        }
        if (index < 0) return NotebookEntry()
        val tokens = lines.drop(index).joinToString(" ").toTokens().dropWhile { !word.matches(it) }
        return read(tokens, lastPaymentDate = null)
    }

    private fun read(tokens: List<String>, lastPaymentDate: LocalDate?): NotebookEntry {
        val name = tokens.takeWhile { word.matches(it) }.take(MAX_NAME_WORDS)
        val afterName = tokens.drop(name.size)
        val figuresStart = afterName.indices.firstOrNull { startsAFigure(afterName, it) } ?: afterName.size
        val description = afterName.take(figuresStart).filter { it.length > 1 || it[0].isDigit() }
        val figures = readFigures(afterName.drop(figuresStart))

        return resolve(
            figures = figures,
            customerName = name.joinToString(" "),
            productDescription = description.joinToString(" "),
            lastPaymentDate = lastPaymentDate,
        )
    }

    /** A dated line opens a layaway, unless it only records another payment ("3-11-26 Abona 10.000"). */
    private fun startOf(line: String): MatchResult? {
        val match = datedLine.matchEntire(line) ?: return null
        val tokens = match.groupValues[2].toTokens()
        val recordsPayment = tokens.firstOrNull()?.let { labelOf(it) } != null
        return match.takeUnless { recordsPayment }
    }

    private fun resolve(
        figures: List<Labeled>,
        customerName: String,
        productDescription: String,
        lastPaymentDate: LocalDate?,
    ): NotebookEntry {
        val labeledTotal = figures.firstOrNull { it.label == Label.Total }?.amount
        val payments = figures.filter { it.label == Label.Paid }.mapNotNull { it.amount }
        val balances = figures.filter { it.label == Label.Balance }.map { it.amount }

        // Without a bare or labeled total, the first balance is the price before any payment was made.
        val total = labeledTotal ?: when {
            balances.size >= 2 -> balances.first()
            balances.size == 1 && payments.isNotEmpty() -> balances.first()?.let { it + payments.last() }
            else -> balances.firstOrNull()
        }
        val balance = if (labeledTotal != null || balances.size >= 2) balances.lastOrNull() else null

        val paid = when {
            payments.size == 1 -> payments.first()
            total != null && balance != null && total >= balance -> total - balance
            else -> payments.lastOrNull()
        }
        return NotebookEntry(customerName, productDescription, total, paid, lastPaymentDate)
    }

    private fun readFigures(tokens: List<String>): List<Labeled> {
        val figures = mutableListOf<Labeled>()
        var pending: Label? = null
        tokens.forEachIndexed { index, token ->
            val label = labelOf(token)
            val amount = parseAmount(token)
            when {
                label != null -> {
                    pending?.let { figures += Labeled(it, null) }
                    pending = label
                }
                amount != null -> {
                    // The price is often written bare, before any "Abona" or "Saldo".
                    val kind = pending ?: if (figures.isEmpty()) Label.Total else return@forEachIndexed
                    figures += Labeled(kind, amount)
                    pending = null
                }
            }
        }
        pending?.let { figures += Labeled(it, null) }
        return figures
    }

    private fun startsAFigure(tokens: List<String>, index: Int): Boolean =
        labelOf(tokens[index]) != null || parseAmount(tokens[index]) != null

    /** "Abona" and "Saldo" are matched even with a letter or two misread; "T" is the shoe size, never the price. */
    private fun labelOf(token: String): Label? {
        if (token == "=") return null
        if (token.equals("total", ignoreCase = true)) return Label.Total
        if (!word.matches(token) || token.length < 4) return null
        val lower = token.lowercase()
        // Short words get less slack, or "alto" would pass for "saldo".
        val allowed = if (token.length >= MIN_LONG_LABEL) MAX_LABEL_DISTANCE else 1
        return when {
            distance(lower, "abona") <= allowed -> Label.Paid
            distance(lower, "saldo") <= allowed -> Label.Balance
            else -> null
        }
    }

    private fun parseAmount(token: String): Money? {
        val separated = separatedAmount.matchEntire(token)
        val digits = when {
            separated != null -> separated.groupValues[1] + separated.groupValues[2]
            plainAmount.matches(token) -> token
            else -> return null
        }
        val corrected = digits.map(::asDigit).joinToString("")
        // Mostly real digits, so words that happen to use look-alike letters are not amounts.
        if (token.count { it.isDigit() } * 2 < token.count { it.isLetterOrDigit() }) return null
        val units = corrected.toLongOrNull() ?: return null
        return Money.fromUnits(units).takeIf { units >= MIN_AMOUNT }
    }

    private fun asDigit(char: Char): Char = when (char) {
        'O', 'o', 'D' -> '0'
        'l', 'I', 'i', '|' -> '1'
        'S', 's' -> '5'
        'B' -> '8'
        'Z' -> '2'
        else -> char
    }

    private fun parseDate(text: String): LocalDate? {
        val digits = text.map { if (it in "Oo") '0' else if (it in "lI|i") '1' else it }.joinToString("")
        val parts = date.matchEntire(digits)?.groupValues ?: return null
        val day = parts[1].toInt()
        val month = parts[2].take(2).toInt()
        val year = parts[3].toInt().let { if (parts[3].length == 2) 2000 + it else it }
        return runCatching { LocalDate(year, month, day) }.getOrNull()
    }

    private fun String.toTokens(): List<String> {
        val words = replace("=", " = ").replace("$", " ").split(Regex("""\s+""")).filter(String::isNotEmpty)
        return mergeSplitThousands(words.flatMap(::splitLabelColon))
    }

    /** "saldo:30.000" is the label "saldo" followed by the amount. */
    private fun splitLabelColon(token: String): List<String> {
        val match = labelWithColon.matchEntire(token) ?: return listOf(token)
        return listOf(match.groupValues[1], match.groupValues[2]).filter(String::isNotEmpty)
    }

    /** Recognition often drops the dot of an amount and leaves "50 000" as two tokens. */
    private fun mergeSplitThousands(tokens: List<String>): List<String> {
        val merged = mutableListOf<String>()
        var index = 0
        while (index < tokens.size) {
            val head = tokens[index]
            val tail = tokens.getOrNull(index + 1)
            val splitAmount = tail != null &&
                thousandsHead.matches(head) && head.any(Char::isDigit) &&
                thousandsTail.matches(tail) && tail.any(Char::isDigit)
            if (splitAmount) {
                merged += "$head.$tail"
                index += 2
            } else {
                merged += head
                index++
            }
        }
        return merged
    }

    private fun distance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, substitution)
            }
            previous = current
        }
        return previous[b.length]
    }
}
