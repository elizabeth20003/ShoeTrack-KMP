package com.shoetrack.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.shoetrack.db.ShoeTrackDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AiKeyRepositoryImplTest {
    private val driver =
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
            ShoeTrackDatabase.Schema.create(it)
        }
    private val repository = AiKeyRepositoryImpl(ShoeTrackDatabase(driver), Dispatchers.Unconfined)

    @Test
    fun hasNoKeyAtFirst() = runBlocking {
        assertNull(repository.key())
    }

    @Test
    fun savesAndReplacesTheKey() = runBlocking {
        repository.save(" first ")
        assertEquals("first", repository.key())

        repository.save("second")
        assertEquals("second", repository.key())
    }

    @Test
    fun aBlankKeyRemovesIt() = runBlocking {
        repository.save("key")

        repository.save("  ")

        assertNull(repository.key())
    }
}
