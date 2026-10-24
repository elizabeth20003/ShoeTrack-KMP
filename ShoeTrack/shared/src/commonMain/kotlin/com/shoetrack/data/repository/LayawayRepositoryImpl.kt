package com.shoetrack.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.shoetrack.data.mapper.layawaySummaryOf
import com.shoetrack.data.mapper.toDbValue
import com.shoetrack.data.mapper.toDomain
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.LayawaySummary
import com.shoetrack.domain.model.Payment
import com.shoetrack.domain.repository.LayawayRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * SQLDelight implementation of [LayawayRepository].
 *
 * Every write flags the record as pending sync and stamps it with [deviceId] and
 * the modification time, so a sync engine can resolve offline edits across devices.
 */
internal class LayawayRepositoryImpl(
    private val database: ShoeTrackDatabase,
    private val deviceId: String,
    private val clock: Clock = Clock.System,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : LayawayRepository {

    private val layawayQueries = database.layawayQueries
    private val paymentQueries = database.paymentQueries

    override suspend fun create(layaway: Layaway) {
        withContext(dispatcher) {
            layawayQueries.insertLayaway(
                layaway.id,
                layaway.customerId,
                layaway.productId,
                layaway.remainingBalance.cents,
                layaway.state.toDbValue(),
                layaway.createdAt.toEpochMilliseconds(),
                layaway.lastPaymentAt.toEpochMilliseconds(),
                clock.now().toEpochMilliseconds(),
                deviceId,
                SYNC_PENDING,
            )
        }
    }

    override suspend fun registerPayment(layaway: Layaway, payment: Payment) {
        require(payment.layawayId == layaway.id) {
            "Payment ${payment.id} belongs to layaway ${payment.layawayId}, not ${layaway.id}"
        }
        withContext(dispatcher) {
            database.transaction {
                paymentQueries.insertPayment(
                    payment.id,
                    payment.layawayId,
                    payment.amount.cents,
                    payment.paidAt.toEpochMilliseconds(),
                    payment.recordedByDeviceId,
                )
                updateRow(layaway)
            }
        }
    }

    override suspend fun updateState(layaway: Layaway) {
        withContext(dispatcher) { updateRow(layaway) }
    }

    override suspend fun getById(id: String): Layaway? = withContext(dispatcher) {
        val row = layawayQueries.selectLayawayById(id).executeAsOneOrNull()
            ?: return@withContext null
        val payments = paymentQueries.selectPaymentsByLayaway(id).executeAsList().map { it.toDomain() }
        row.toDomain(payments)
    }

    override suspend fun getByState(state: LayawayState): List<Layaway> =
        withContext(dispatcher) {
            layawayQueries.selectLayawaysByState(state.toDbValue())
                .executeAsList()
                .map { it.toDomain() }
        }

    override fun observeSummaries(): Flow<List<LayawaySummary>> =
        layawayQueries.selectAllSummaries(mapper = ::layawaySummaryOf)
            .asFlow()
            .mapToList(dispatcher)

    override suspend fun getPendingSync(): List<Layaway> = withContext(dispatcher) {
        layawayQueries.selectPendingSync().executeAsList().map { it.toDomain() }
    }

    private fun updateRow(layaway: Layaway) {
        layawayQueries.updateLayaway(
            layaway.remainingBalance.cents,
            layaway.state.toDbValue(),
            layaway.lastPaymentAt.toEpochMilliseconds(),
            clock.now().toEpochMilliseconds(),
            deviceId,
            layaway.id,
        )
    }

    private companion object {
        const val SYNC_PENDING = 1L
    }
}
