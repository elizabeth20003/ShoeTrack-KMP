package com.shoetrack.data.sync

import com.shoetrack.domain.sync.PairingCode
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
 *
 * iOS does not allow the broadcast without a restricted entitlement, so a device can also be
 * paired by hand with a [PairingCode] (shown as a QR). Paired devices are synced on every round.
 */
internal class LanSyncEngine(
    private val deviceId: String,
    private val session: SyncSession,
    private val clock: Clock = Clock.System,
) : SyncEngine {

    private val _status = MutableStateFlow(SyncStatus())
    override val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _pairingCode = MutableStateFlow<PairingCode?>(null)
    override val pairingCode: StateFlow<PairingCode?> = _pairingCode.asStateFlow()

    private var scope: CoroutineScope? = null
    private var selector: SelectorManager? = null
    private val peers = mutableMapOf<String, Peer>()
    private val peersLock = Mutex()
    private val activeSyncs = mutableSetOf<String>()
    private var lastServedAt: Instant? = null

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
                launch { publishPairingCode(port) }
                launch { syncPairedPeers() }
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
        _pairingCode.value = null
        // Paired devices are kept: the next start() syncs with them again.
    }

    override suspend fun pairWith(code: PairingCode): Boolean {
        val peer = Peer(
            deviceId = "paired:${code.host}:${code.port}",
            host = code.host,
            port = code.port,
            lastSeen = clock.now(),
            paired = true,
        )
        peersLock.withLock { peers.getOrPut(peer.deviceId) { peer } }
        val synced = syncWith(peer)
        // A code that leads nowhere must not be retried forever.
        if (!synced) peersLock.withLock { peers.remove(peer.deviceId) }
        return synced
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
            peersLock.withLock { lastServedAt = clock.now() }
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

    private suspend fun syncWith(peer: Peer): Boolean {
        val started = peersLock.withLock { activeSyncs.add(peer.deviceId) }
        if (!started) return false
        var succeeded = false
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
            succeeded = true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _status.update { it.copy(hasFailed = true) }
        } finally {
            socket?.close()
            peersLock.withLock {
                activeSyncs -= peer.deviceId
                publishPeerCount()
            }
            _status.update { it.copy(isSyncing = false) }
        }
        return succeeded
    }

    private suspend fun publishPairingCode(port: Int) {
        while (true) {
            _pairingCode.value = localIpv4Address()?.let { PairingCode(it, port) }
            delay(ANNOUNCE_INTERVAL)
        }
    }

    private suspend fun syncPairedPeers() {
        while (true) {
            delay(SYNC_INTERVAL)
            val paired = peersLock.withLock { peers.values.filter { it.paired } }
            paired.forEach { peer -> scope?.launch { syncWith(peer) } }
        }
    }

    private suspend fun forgetSilentPeers() {
        while (true) {
            delay(ANNOUNCE_INTERVAL)
            peersLock.withLock {
                val limit = clock.now() - PEER_TIMEOUT
                peers.values.removeAll { !it.paired && it.lastSeen < limit }
                publishPeerCount()
            }
        }
    }

    // Callers hold peersLock.
    private fun publishPeerCount() {
        val now = clock.now()
        val reachable = peers.values.count { !it.paired || it.lastSyncAt?.let { at -> now - at < PAIRED_TIMEOUT } == true }
        // A device that paired with us only shows up as a connection we served.
        val served = lastServedAt?.let { now - it < PAIRED_TIMEOUT } == true
        _status.update { it.copy(nearbyDevices = if (reachable == 0 && served) 1 else reachable) }
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
        val paired: Boolean = false,
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
        val PAIRED_TIMEOUT = 30.seconds
    }
}
