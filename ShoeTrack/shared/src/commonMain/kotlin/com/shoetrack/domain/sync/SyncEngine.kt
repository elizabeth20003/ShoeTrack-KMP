package com.shoetrack.domain.sync

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

/** What the user can see about synchronisation with the other devices of the store. */
data class SyncStatus(
    val nearbyDevices: Int = 0,
    val isSyncing: Boolean = false,
    val lastSyncAt: Instant? = null,
    val hasFailed: Boolean = false,
)

/** Finds the other devices of the store and keeps their data in step while the app is open. */
interface SyncEngine {
    val status: StateFlow<SyncStatus>

    /** How other devices can reach this one, or null while the device has no local network address. */
    val pairingCode: StateFlow<PairingCode?>

    fun start()

    fun stop()

    /** Connects to the device behind [code] and keeps syncing with it. Returns whether the first sync worked. */
    suspend fun pairWith(code: PairingCode): Boolean
}
