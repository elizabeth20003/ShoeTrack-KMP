package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.Product
import com.shoetrack.domain.model.SyncSnapshot
import com.shoetrack.domain.repository.SyncRepository
import com.shoetrack.domain.sync.LayawayMerger
import kotlin.time.Clock

/** What a merge changed on this device. */
data class MergeResult(
    val changedLayawayIds: List<String>,
    /** Photos of products that arrived and may not be on this device yet. */
    val newPhotoNames: Set<String>,
) {
    val hasChanges: Boolean get() = changedLayawayIds.isNotEmpty()
}

/** Applies the data received from another device to this one. Running it twice changes nothing. */
class MergeSnapshotUseCase(
    private val syncRepository: SyncRepository,
    private val merger: LayawayMerger,
    private val evaluateOverdue: EvaluateOverdueUseCase,
    private val scheduleReminder: ScheduleOverdueReminderUseCase,
    private val clock: Clock,
) {

    suspend operator fun invoke(remote: SyncSnapshot): MergeResult {
        val local = syncRepository.snapshot()
        val localCustomers = local.customers.associateBy(Customer::id)
        val localProducts = local.products.associateBy(Product::id)
        val localLayaways = local.layaways.associateBy(Layaway::id)

        val newCustomers = remote.customers.filter { it.id !in localCustomers }
        val newProducts = remote.products.filter { it.id !in localProducts }
        val knownProducts = localProducts + newProducts.associateBy(Product::id)
        val knownCustomerIds = localCustomers.keys + newCustomers.map(Customer::id)

        val now = clock.now()
        val changedLayaways = remote.layaways.mapNotNull { incoming ->
            // A layaway whose customer or product did not arrive cannot be shown; skip it.
            val product = knownProducts[incoming.productId] ?: return@mapNotNull null
            if (incoming.customerId !in knownCustomerIds) return@mapNotNull null

            val existing = localLayaways[incoming.id]
            val merged = evaluateOverdue(merger.merge(existing, incoming, product.totalPrice), now)
            merged.takeIf { existing == null || !it.hasSameContentAs(existing) }
        }

        if (newCustomers.isEmpty() && newProducts.isEmpty() && changedLayaways.isEmpty()) {
            return MergeResult(emptyList(), emptySet())
        }

        syncRepository.store(SyncSnapshot(newCustomers, newProducts, changedLayaways))
        changedLayaways.forEach { scheduleReminder(it.id) }

        return MergeResult(
            changedLayawayIds = changedLayaways.map(Layaway::id),
            newPhotoNames = newProducts.map(Product::photoPath).filter(String::isNotBlank).toSet(),
        )
    }

    private fun Layaway.hasSameContentAs(other: Layaway): Boolean =
        state == other.state &&
            remainingBalance == other.remainingBalance &&
            payments.map { it.id }.toSet() == other.payments.map { it.id }.toSet()
}
