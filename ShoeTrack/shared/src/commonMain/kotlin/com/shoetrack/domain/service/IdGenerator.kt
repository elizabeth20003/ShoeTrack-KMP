package com.shoetrack.domain.service

/** Produces unique identifiers for new records, so use cases stay deterministic in tests. */
interface IdGenerator {
    fun newId(): String
}
