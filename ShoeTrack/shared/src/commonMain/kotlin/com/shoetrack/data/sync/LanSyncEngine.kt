package com.shoetrack.data.sync

import com.shoetrack.domain.sync.SyncEngine
import com.shoetrack.domain.sync.SyncStatus
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.core.writeText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Syncs with the other devices on the same Wi-Fi network, with no server and no internet.
 *
 * Every device announces itself by UDP broadcast and listens for TCP connections. When two
 * devices see each other, the one with the smaller id opens the connection, so a pair never
 * syncs twice at once.
 */
internal class LanSyncEngine(
    private val deviceId: String,
    private val session: SyncSession,
    private val clock: Clock = Clock.System,
) : SyncEngine {

    private val _status = MutableStateFlow(SyncStatus())
    override val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private var scope: CoroutineScope? = null
    private var selector: SelectorManager? = null
    private val peers = mutableMapOf<String, Peer>()
    private val peersLock = Mutex()
    private val activeSyncs = mutableSetOf<String>()

    override fun start() {
        if (scope != null) return
        val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = engineScope
        engineScope.launch {
            val selector = SelectorManager(Dispatchers.Default).also { selector = it }
            try {
                val server = aSocket(selector).tcp().bind(InetSocketAddress("0.0.0.0", 0))
                val port = (server.localAddress as InetSocketAddress).port
                launch { acceptConnections(server) }
                launch { announce(selector, port) }
                launch { listenForPeers(selector) }
                launch { forgetSilentPeers() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _status.update { it.copy(hasFailed = true) }
            }
        }
    }

    override fun stop() {
        scope?.cancel()
        scope = null
        selector?.close()
        selector = null
        _status.value = SyncStatus()
    }

    private suspend fun acceptConnections(server: ServerSocket) {
        while (true) {
            val socket = server.accept()
            scope?.launch { serve(socket) }
        }
    }

    private suspend fun serve(socket: Socket) {
        try {
            withTimeout(SESSION_TIMEOUT) {
                session.runAsResponder(SocketMessageChannel(socket.openReadChannel(), socket.openWriteChannel(autoFlush = false)))
            }
            markSynced()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The other device left the network or sent something unexpected; the next round retries.
        } finally {
            socket.close()
        }
    }

    private suspend fun announce(selector: SelectorManager, tcpPort: Int) {
        val socket = aSocket(selector).udp().bind(InetSocketAddress("0.0.0.0", 0)) { broadcast = true }
        val message = Json.encodeToString(Announcement.serializer(), Announcement(deviceId, tcpPort))
        while (true) {
            try {
                socket.send(Datagram(buildPacket { writeText(message) }, InetSocketAddress(BROADCAST_ADDRESS, DISCOVERY_PORT)))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // No network at the moment; try again on the next round.
            }
            delay(ANNOUNCE_INTERVAL)
        }
    }

    private suspend fun listenForPeers(selector: SelectorManager) {
        val socket = aSocket(selector).udp().bind(InetSocketAddress("0.0.0.0", DISCOVERY_PORT)) {
            reuseAddress = true
            broadcast = true
        }
        for (datagram in socket.incoming) {
            val sender = datagram.address as? InetSocketAddress ?: continue
            val announcement = runCatching {
                Json.decodeFromString(Announcement.serializer(), datagram.packet.readText())
            }.getOrNull() ?: continue
            if (announcement.deviceId == deviceId) continue

            onPeerSeen(Peer(announcement.deviceId, sender.hostname, announcement.port, clock.now()))
        }
    }

    private suspend fun onPeerSeen(seen: Peer) {
        val shouldSync = peersLock.withLock {
            val known = peers[seen.deviceId]
            val updated = seen.copy(lastSyncAt = known?.lastSyncAt)
            peers[seen.deviceId] = updated
            publishPeerCount()
            deviceId < seen.deviceId &&
                seen.deviceId !in activeSyncs &&
                (updated.lastSyncAt == null || clock.now() - updated.lastSyncAt >= SYNC_INTERVAL)
        }
        if (shouldSync) scope?.launch { syncWith(seen) }
    }

    private suspend fun syncWith(peer: Peer) {
        peersLock.withLock { activeSyncs += peer.deviceId }
        _status.update { it.copy(isSyncing = true) }
        var socket: Socket? = null
        try {
            val selector = checkNotNull(selector) { "The sync engine is not running" }
            socket = aSocket(selector).tcp().connect(InetSocketAddress(peer.host, peer.port))
            withTimeout(SESSION_TIMEOUT) {
                session.runAsInitiator(SocketMessageChannel(socket.openReadChannel(), socket.openWriteChannel(autoFlush = false)))
            }
            peersLock.withLock { peers[peer.deviceId]?.let { peers[peer.deviceId] = it.copy(lastSyncAt = clock.now()) } }
            markSynced()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _status.update { it.copy(hasFailed = true) }
        } finally {
            socket?.close()
            peersLock.withLock { activeSyncs -= peer.deviceId }
            _status.update { it.copy(isSyncing = false) }
        }
    }

    private suspend fun forgetSilentPeers() {
        while (true) {
            delay(ANNOUNCE_INTERVAL)
            peersLock.withLock {
                val limit = clock.now() - PEER_TIMEOUT
                peers.values.removeAll { it.lastSeen < limit }
                publishPeerCount()
            }
        }
    }

    private fun publishPeerCount() {
        _status.update { it.copy(nearbyDevices = peers.size) }
    }

    private fun markSynced() {
        _status.update { it.copy(lastSyncAt = clock.now(), hasFailed = false) }
    }

    private data class Peer(
        val deviceId: String,
        val host: String,
        val port: Int,
        val lastSeen: Instant,
        val lastSyncAt: Instant? = null,
    )

    @Serializable
    private data class Announcement(val deviceId: String, val port: Int)

    private companion object {
        const val DISCOVERY_PORT = 45871
        const val BROADCAST_ADDRESS = "255.255.255.255"
        val ANNOUNCE_INTERVAL = 3.seconds
        val SYNC_INTERVAL = 10.seconds
        val PEER_TIMEOUT = 12.seconds
        val SESSION_TIMEOUT = 60.seconds
    }
}
