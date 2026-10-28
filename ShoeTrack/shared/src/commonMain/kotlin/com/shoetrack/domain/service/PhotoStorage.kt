package com.shoetrack.domain.service

/** Keeps photos on the device. Photos are referred to by name, never by absolute path. */
interface PhotoStorage {
    /** Stores the image and returns the name to keep in the database. */
    suspend fun save(imageBytes: ByteArray): String

    suspend fun load(name: String): ByteArray?

    suspend fun exists(name: String): Boolean

    /** Stores a photo received from another device under the name its product already refers to. */
    suspend fun saveAs(name: String, imageBytes: ByteArray)
}
