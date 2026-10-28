package com.shoetrack.data.sync

import com.shoetrack.domain.repository.SyncRepository
import com.shoetrack.domain.service.PhotoNames
import com.shoetrack.domain.service.PhotoStorage
import com.shoetrack.domain.usecase.MergeSnapshotUseCase
import kotlin.io.encoding.Base64

/**
 * One exchange between two devices. The device that opened the connection speaks first.
 *
 *  1. Initiator sends its data; the responder merges it and answers with the merged result,
 *     which the initiator merges too, so both end up identical.
 *  2. Each side in turn asks for the photos it lacks and the other one serves them.
 */
internal class SyncSession(
    private val syncRepository: SyncRepository,
    private val mergeSnapshot: MergeSnapshotUseCase,
    private val photoStorage: PhotoStorage,
) {

    suspend fun runAsInitiator(channel: MessageChannel) {
        channel.send(SnapshotMessage(syncRepository.snapshot().toDto()))
        merge(channel.receive())

        requestMissingPhotos(channel)
        servePhotos(channel)
    }

    suspend fun runAsResponder(channel: MessageChannel) {
        merge(channel.receive())
        channel.send(SnapshotMessage(syncRepository.snapshot().toDto()))

        servePhotos(channel)
        requestMissingPhotos(channel)
    }

    private suspend fun merge(message: SyncMessage) {
        val snapshot = (message as? SnapshotMessage ?: unexpected(message)).snapshot
        mergeSnapshot(snapshot.toDomain())
    }

    private suspend fun requestMissingPhotos(channel: MessageChannel) {
        val missing = syncRepository.snapshot().products
            .map { it.photoPath }
            .filter { PhotoNames.isValid(it) && !photoStorage.exists(it) }
            .distinct()

        for (name in missing) {
            channel.send(PhotoRequestMessage(name))
            val received = channel.receive()
            val reply = received as? PhotoMessage ?: unexpected(received)
            if (reply.name == name && reply.base64 != null) {
                photoStorage.saveAs(name, Base64.decode(reply.base64))
            }
        }
        channel.send(DoneMessage)
    }

    private suspend fun servePhotos(channel: MessageChannel) {
        while (true) {
            when (val message = channel.receive()) {
                is PhotoRequestMessage -> {
                    val bytes = if (PhotoNames.isValid(message.name)) photoStorage.load(message.name) else null
                    channel.send(PhotoMessage(message.name, bytes?.let { Base64.encode(it) }))
                }
                DoneMessage -> return
                else -> unexpected(message)
            }
        }
    }

    private fun unexpected(message: SyncMessage): Nothing =
        throw IllegalStateException("Unexpected sync message: $message")
}
