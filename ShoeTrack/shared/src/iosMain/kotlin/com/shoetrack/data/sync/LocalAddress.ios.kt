package com.shoetrack.data.sync

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.posix.AF_INET
import platform.posix.freeifaddrs
import platform.posix.getifaddrs
import platform.posix.ifaddrs
import platform.posix.sockaddr_in

private const val INTERFACE_UP = 0x1u
private const val INTERFACE_LOOPBACK = 0x8u

// "en" is Wi-Fi and Ethernet; "bridge" is the Personal Hotspot. Cellular interfaces are "pdp_ip".
private val LOCAL_INTERFACE_PREFIXES = listOf("en", "bridge")

@OptIn(ExperimentalForeignApi::class)
internal actual fun localIpv4Address(): String? = memScoped {
    val first = alloc<CPointerVar<ifaddrs>>()
    if (getifaddrs(first.ptr) != 0) return@memScoped null
    try {
        var cursor: CPointer<ifaddrs>? = first.value
        while (cursor != null) {
            val entry = cursor.pointed
            val address = entry.ifa_addr
            val name = entry.ifa_name?.toKString().orEmpty()
            val usable = (entry.ifa_flags and INTERFACE_UP) != 0u && (entry.ifa_flags and INTERFACE_LOOPBACK) == 0u
            if (address != null && usable && address.pointed.sa_family.toInt() == AF_INET &&
                LOCAL_INTERFACE_PREFIXES.any { name.startsWith(it) }
            ) {
                // s_addr is in network order, so on these little-endian devices the first octet is the lowest byte.
                val raw = address.reinterpret<sockaddr_in>().pointed.sin_addr.s_addr
                return@memScoped "${raw and 0xFFu}.${(raw shr 8) and 0xFFu}.${(raw shr 16) and 0xFFu}.${(raw shr 24) and 0xFFu}"
            }
            cursor = entry.ifa_next
        }
        null
    } finally {
        freeifaddrs(first.value)
    }
}
