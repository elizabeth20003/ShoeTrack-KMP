package com.shoetrack.domain.sync

/**
 * Where another device can reach this one on the local network. It is what the pairing QR
 * carries, so a device that cannot discover its neighbours on its own (iOS blocks the
 * broadcast discovery) can still connect to them.
 */
data class PairingCode(val host: String, val port: Int) {
    fun encode(): String = "$PREFIX$host:$port"

    companion object {
        private const val PREFIX = "shoetrack:1:"
        private const val OCTET_COUNT = 4
        private const val MAX_OCTET = 255
        private const val MAX_PORT = 65_535

        /** Returns null when [text] is not a code made by [encode]. */
        fun parse(text: String): PairingCode? {
            val trimmed = text.trim()
            if (!trimmed.startsWith(PREFIX)) return null
            val parts = trimmed.removePrefix(PREFIX).split(':')
            if (parts.size != 2) return null
            val octets = parts[0].split('.')
            val validHost =
                octets.size == OCTET_COUNT &&
                    octets.all { octet -> octet.toIntOrNull()?.let { it in 0..MAX_OCTET } == true }
            val port = parts[1].toIntOrNull()?.takeIf { it in 1..MAX_PORT }
            return if (validHost && port != null) PairingCode(parts[0], port) else null
        }
    }
}
