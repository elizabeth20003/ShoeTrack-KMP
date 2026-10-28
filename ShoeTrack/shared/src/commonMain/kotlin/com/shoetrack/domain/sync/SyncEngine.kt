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

    fun start()

    fun stop()
}
