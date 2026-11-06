package com.shoetrack.data.sync

/** The IPv4 address of this device on the Wi-Fi or Ethernet network, or null when it is on none. */
internal expect fun localIpv4Address(): String?
