package com.shoetrack.data.sync

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel

import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeInt
import kotlinx.serialization.json.Json

/** A two-way line of messages with another device. */
internal interface MessageChannel {
    suspend fun send(message: SyncMessage)

    suspend fun receive(): SyncMessage
}

private val json = Json { ignoreUnknownKeys = true }

/** Largest message accepted, so a faulty or hostile peer cannot make the app allocate unbounded memory. */
private const val MAX_MESSAGE_BYTES = 32 * 1024 * 1024

/** Sends each message as a JSON text preceded by its length in bytes. */
internal class SocketMessageChannel(
    private val input: ByteReadChannel,
    private val output: ByteWriteChannel,
) : MessageChannel {

    override suspend fun send(message: SyncMessage) {
        val bytes = json.encodeToString(SyncMessage.serializer(), message).encodeToByteArray()
        output.writeInt(bytes.size)
        output.writeFully(bytes)
        output.flush()
    }

    override suspend fun receive(): SyncMessage {
        val length = input.readInt()
        require(length in 1..MAX_MESSAGE_BYTES) { "Invalid message length: $length" }
        val bytes = input.readByteArray(length)
        return json.decodeFromString(SyncMessage.serializer(), bytes.decodeToString())
    }
}
