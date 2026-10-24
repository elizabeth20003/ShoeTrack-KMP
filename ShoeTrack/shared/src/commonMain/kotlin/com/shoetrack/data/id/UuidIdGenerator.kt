package com.shoetrack.data.id

import com.shoetrack.domain.service.IdGenerator
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class UuidIdGenerator : IdGenerator {
    override fun newId(): String = Uuid.random().toString()
}
