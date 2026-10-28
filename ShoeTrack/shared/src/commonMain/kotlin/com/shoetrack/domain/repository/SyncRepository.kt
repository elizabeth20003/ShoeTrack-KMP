package com.shoetrack.domain.repository

import com.shoetrack.domain.model.SyncSnapshot

interface SyncRepository {

    suspend fun snapshot(): SyncSnapshot

    /**
     * Writes [changes] in one transaction. Customers, products and payments that already
     * exist are left untouched; layaways are replaced by the given version.
     */
    suspend fun store(changes: SyncSnapshot)
}
