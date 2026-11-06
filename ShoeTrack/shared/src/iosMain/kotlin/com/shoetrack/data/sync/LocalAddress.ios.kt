package com.shoetrack.data.sync

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers

// The system does not list its interfaces to Kotlin on iOS, so ask it which address it would send
// from. Connecting a UDP socket sends nothing: it only makes the system pick the outgoing interface.
private const val PROBE_HOST = "8.8.8.8"
private const val PROBE_PORT = 53
private const val UNSPECIFIED_ADDRESS = "0.0.0.0"

internal actual suspend fun localIpv4Address(): String? {
    val selector = SelectorManager(Dispatchers.Default)
    return try {
        val socket = aSocket(selector).udp().connect(InetSocketAddress(PROBE_HOST, PROBE_PORT))
        try {
            (socket.localAddress as? InetSocketAddress)?.hostname?.takeIf { it != UNSPECIFIED_ADDRESS }
        } finally {
            socket.close()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    } finally {
        selector.close()
    }
}
