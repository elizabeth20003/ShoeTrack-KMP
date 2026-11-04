package com.shoetrack.domain.repository

/** Keeps the API key of the cloud service that reads handwriting; it never leaves this device. */
interface AiKeyRepository {
    suspend fun key(): String?

    /** Saves [key], or removes it when it is null or blank. */
    suspend fun save(key: String?)
}
