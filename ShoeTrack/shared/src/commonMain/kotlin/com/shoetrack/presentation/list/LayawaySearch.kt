package com.shoetrack.presentation.list

import com.shoetrack.domain.model.LayawaySummary
import com.shoetrack.domain.search.matchesAllWords

/** True when every word of [query] appears in the customer name or the shoe description. */
internal fun LayawaySummary.matches(query: String): Boolean =
    matchesAllWords(query, "$customerName $productDescription")
