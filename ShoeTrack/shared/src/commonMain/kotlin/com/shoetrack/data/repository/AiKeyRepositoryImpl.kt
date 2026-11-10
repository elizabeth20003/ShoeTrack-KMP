package com.shoetrack.data.repository

import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.repository.AiKeyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AiKeyRepositoryImpl(database: ShoeTrackDatabase, private val dispatcher: CoroutineDispatcher = Dispatchers.Default) :
    AiKeyRepository {
    private val queries = database.settingQueries

    override suspend fun key(): String? = withContext(dispatcher) {
        queries.selectSetting(KEY_NAME).executeAsOneOrNull()
    }

    override suspend fun save(key: String?) {
        withContext(dispatcher) {
            val trimmed = key?.trim().orEmpty()
            if (trimmed.isEmpty()) queries.deleteSetting(KEY_NAME) else queries.upsertSetting(KEY_NAME, trimmed)
        }
    }

    private companion object {
        const val KEY_NAME = "ai_api_key"
    }
}
