package com.shoetrack.data.repository

import com.shoetrack.data.mapper.toDbValue
import com.shoetrack.data.mapper.toDomain
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.model.SyncSnapshot
import com.shoetrack.domain.repository.SyncRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock

internal class SyncRepositoryImpl(
    private val database: ShoeTrackDatabase,
    private val deviceId: String,
    private val clock: Clock = Clock.System,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SyncRepository {

    override suspend fun snapshot(): SyncSnapshot = withContext(dispatcher) {
        val paymentsByLayaway = database.paymentQueries.selectAllPayments()
            .executeAsList()
            .map { it.toDomain() }
            .groupBy { it.layawayId }

        SyncSnapshot(
            customers = database.customerQueries.selectAllCustomers().executeAsList().map { it.toDomain() },
            products = database.productQueries.selectAllProducts().executeAsList().map { it.toDomain() },
            layaways = database.layawayQueries.selectAllLayaways().executeAsList().map {
                it.toDomain(paymentsByLayaway[it.id].orEmpty())
            },
        )
    }

    override suspend fun store(changes: SyncSnapshot) {
        withContext(dispatcher) {
            database.transaction {
                changes.customers.forEach {
                    database.customerQueries.insertCustomerIfAbsent(
                        it.id, it.name, it.phone, it.registeredAt.toEpochMilliseconds(),
                    )
                }
                changes.products.forEach {
                    database.productQueries.insertProductIfAbsent(
                        it.id, it.description, it.photoPath, it.totalPrice.cents,
                    )
                }
                changes.layaways.forEach { layaway ->
                    val modifiedAt = clock.now().toEpochMilliseconds()
                    database.layawayQueries.insertSyncedLayawayIfAbsent(
                        layaway.id,
                        layaway.customerId,
                        layaway.productId,
                        layaway.remainingBalance.cents,
                        layaway.state.toDbValue(),
                        layaway.createdAt.toEpochMilliseconds(),
                        layaway.lastPaymentAt.toEpochMilliseconds(),
                        modifiedAt,
                        deviceId,
                    )
                    database.layawayQueries.updateSyncedLayaway(
                        layaway.remainingBalance.cents,
                        layaway.state.toDbValue(),
                        layaway.lastPaymentAt.toEpochMilliseconds(),
                        modifiedAt,
                        deviceId,
                        layaway.id,
                    )
                    layaway.payments.forEach {
                        database.paymentQueries.insertPaymentIfAbsent(
                            it.id,
                            it.layawayId,
                            it.amount.cents,
                            it.paidAt.toEpochMilliseconds(),
                            it.recordedByDeviceId,
                        )
                    }
                }
            }
        }
    }
}
