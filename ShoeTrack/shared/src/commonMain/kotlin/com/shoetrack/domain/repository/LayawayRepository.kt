package com.shoetrack.domain.repository

import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Payment
import kotlinx.coroutines.flow.Flow

/**
 * Persistence contract for layaways.
 *
 * Lists from [observeAll] and [getByState] come with an empty `payments`
 * to stay light; use [getById] for the full payment history.
 */
interface LayawayRepository {

    suspend fun create(layaway: Layaway)

    /** Saves the [payment] and the already-updated [layaway] in one transaction. */
    suspend fun registerPayment(layaway: Layaway, payment: Payment)

    /** Persists a state change not caused by a payment, such as becoming overdue. */
    suspend fun updateState(layaway: Layaway)

    suspend fun getById(id: String): Layaway?

    suspend fun getByState(state: LayawayState): List<Layaway>

    /** Emits all layaways, most recent payment first, whenever any of them changes. */
    fun observeAll(): Flow<List<Layaway>>

    /** Layaways with local changes not yet synced to other devices. */
    suspend fun getPendingSync(): List<Layaway>
}
