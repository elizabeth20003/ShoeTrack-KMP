package com.shoetrack.presentation.list

import com.shoetrack.domain.model.LayawaySummary

/** True when every word of [query] appears in the customer name or the shoe description. */
internal fun LayawaySummary.matches(query: String): Boolean {
    val words = query.normalizedForSearch().split(' ').filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val text = "$customerName $productDescription".normalizedForSearch()
    return words.all { it in text }
}

private const val WITH_ACCENTS = "áàäâéèëêíìïîóòöôúùüûñ"
private const val WITHOUT_ACCENTS = "aaaaeeeeiiiioooouuuun"

private fun String.normalizedForSearch(): String = lowercase().map { character ->
    val accent = WITH_ACCENTS.indexOf(character)
    if (accent >= 0) WITHOUT_ACCENTS[accent] else character
}.joinToString("")
