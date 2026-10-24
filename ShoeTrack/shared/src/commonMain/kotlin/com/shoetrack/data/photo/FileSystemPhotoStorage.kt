package com.shoetrack.data.photo

import com.shoetrack.domain.service.IdGenerator
import com.shoetrack.domain.service.PhotoStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

/**
 * Stores photos as JPEG files inside [photosDirectory]. Only the file name is returned,
 * because the absolute location of an app's folder can change between launches on iOS.
 */
internal class FileSystemPhotoStorage(
    private val photosDirectory: String,
    private val idGenerator: IdGenerator,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : PhotoStorage {

    override suspend fun save(imageBytes: ByteArray): String = withContext(dispatcher) {
        SystemFileSystem.createDirectories(Path(photosDirectory))
        val name = "${idGenerator.newId()}.jpg"
        SystemFileSystem.sink(Path(photosDirectory, name)).buffered().use { it.write(imageBytes) }
        name
    }

    override suspend fun load(name: String): ByteArray? = withContext(dispatcher) {
        val file = Path(photosDirectory, name)
        if (!SystemFileSystem.exists(file)) {
            null
        } else {
            SystemFileSystem.source(file).buffered().use { it.readByteArray() }
        }
    }
}
