package com.shoetrack.domain.search

/** True when every word of [query] appears in [text], ignoring case and accents. An empty query matches everything. */
fun matchesAllWords(query: String, text: String): Boolean {
    val words = query.normalizedForSearch().split(' ').filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val normalizedText = text.normalizedForSearch()
    return words.all { it in normalizedText }
}

private const val WITH_ACCENTS = "áàäâéèëêíìïîóòöôúùüûñ"
private const val WITHOUT_ACCENTS = "aaaaeeeeiiiioooouuuun"

private fun String.normalizedForSearch(): String = lowercase().map { character ->
    val accent = WITH_ACCENTS.indexOf(character)
    if (accent >= 0) WITHOUT_ACCENTS[accent] else character
}.joinToString("")
