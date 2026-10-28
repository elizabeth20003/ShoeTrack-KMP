package com.shoetrack.domain.service

/** Photo names come from other devices when syncing, so they must never be able to point outside the photos folder. */
object PhotoNames {
    private val valid = Regex("[A-Za-z0-9-]{1,64}\\.jpg")

    fun isValid(name: String): Boolean = valid.matches(name)
}
