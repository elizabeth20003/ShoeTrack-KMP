package com.shoetrack.data.sync

import java.net.Inet4Address
import java.net.NetworkInterface

// Mobile data interfaces also have private addresses, so only the Wi-Fi and Ethernet ones count.
private val LOCAL_INTERFACE_PREFIXES = listOf("wlan", "swlan", "ap", "eth")

internal actual fun localIpv4Address(): String? = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { network -> network.isUp && LOCAL_INTERFACE_PREFIXES.any { network.name.startsWith(it) } }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull { it.isSiteLocalAddress }
        ?.hostAddress
}.getOrNull()
