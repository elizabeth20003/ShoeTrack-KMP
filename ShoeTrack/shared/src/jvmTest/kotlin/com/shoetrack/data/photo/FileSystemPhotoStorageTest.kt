package com.shoetrack.data.photo

import com.shoetrack.domain.fakes.SequentialIdGenerator
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FileSystemPhotoStorageTest {

    private val directory = Files.createTempDirectory("shoetrack-photos").resolve("photos")
    private val storage = FileSystemPhotoStorage(directory.toString(), SequentialIdGenerator())

    @Test
    fun savedPhotoCanBeLoadedByItsName() = runTest {
        val image = byteArrayOf(10, 20, 30, 40)

        val name = storage.save(image)

        assertContentEquals(image, storage.load(name))
    }

    @Test
    fun returnsOnlyTheFileNameNotAPath() = runTest {
        assertEquals("id-1.jpg", storage.save(byteArrayOf(1)))
    }

    @Test
    fun loadingAMissingPhotoReturnsNull() = runTest {
        assertNull(storage.load("missing.jpg"))
    }
}
