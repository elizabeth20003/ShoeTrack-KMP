package com.shoetrack.domain.usecase

import com.shoetrack.domain.fakes.FakeCustomerRepository
import com.shoetrack.domain.fakes.FakeLayawayRepository
import com.shoetrack.domain.fakes.FakeOverdueReminderScheduler
import com.shoetrack.domain.fakes.FakeProductRepository
import com.shoetrack.domain.fakes.FakeSyncRepository
import com.shoetrack.domain.fakes.FixedClock
import com.shoetrack.domain.fakes.scheduleReminderOf
import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.OverdueReminder
import com.shoetrack.domain.model.Payment
import com.shoetrack.domain.model.Product
import com.shoetrack.domain.model.SyncSnapshot
import com.shoetrack.domain.sync.LayawayMerger
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class MergeSnapshotUseCaseTest {

    private val start = Instant.parse("2026-01-01T00:00:00Z")
    private val customers = FakeCustomerRepository()
    private val products = FakeProductRepository()
    private val layaways = FakeLayawayRepository()
    private val scheduler = FakeOverdueReminderScheduler()
    private val mergeSnapshot = MergeSnapshotUseCase(
        syncRepository = FakeSyncRepository(layaways, customers, products),
        merger = LayawayMerger(),
        evaluateOverdue = EvaluateOverdueUseCase(),
        scheduleReminder = scheduleReminderOf(layaways, customers, products, scheduler),
        clock = FixedClock(start + 1.days),
    )

    private val customer = Customer("customer-1", "Ana Perez", "3001234567", start)
    private val product = Product("product-1", "Nike Air Force 42", "photo-1.jpg", Money(10_000))

    private fun layaway(vararg payments: Payment, state: LayawayState = LayawayState.Active): Layaway {
        val paid = payments.fold(Money.ZERO) { total, payment -> total + payment.amount }
        return Layaway(
            id = "layaway-1",
            customerId = customer.id,
            productId = product.id,
            remainingBalance = product.totalPrice - paid,
            state = state,
            createdAt = start,
            lastPaymentAt = payments.maxOfOrNull { it.paidAt } ?: start,
            payments = payments.toList(),
        )
    }

    private fun payment(id: String, amount: Long, day: Int, device: String) =
        Payment(id, "layaway-1", Money(amount), start + day.days, device)

    private fun remoteWith(layaway: Layaway) =
        SyncSnapshot(listOf(customer), listOf(product), listOf(layaway))

    private fun storeLocally(layaway: Layaway) {
        customers.customers[customer.id] = customer
        products.products[product.id] = product
        layaways.layaways[layaway.id] = layaway
    }

    @Test
    fun storesDataThatThisDeviceDidNotHave() = runTest {
        val result = mergeSnapshot(remoteWith(layaway(payment("p1", 2_000, 0, "device-b"))))

        assertEquals(listOf("layaway-1"), result.changedLayawayIds)
        assertEquals(setOf("photo-1.jpg"), result.newPhotoNames)
        assertNotNull(customers.customers[customer.id])
        assertEquals(Money(8_000), layaways.layaways.getValue("layaway-1").remainingBalance)
    }

    @Test
    fun combinesPaymentsRecordedAtTheSameTimeOnBothDevices() = runTest {
        storeLocally(layaway(payment("p1", 2_000, 0, "device-a")))

        mergeSnapshot(remoteWith(layaway(payment("p2", 3_000, 0, "device-b"))))

        val merged = layaways.layaways.getValue("layaway-1")
        assertEquals(setOf("p1", "p2"), merged.payments.map { it.id }.toSet())
        assertEquals(Money(5_000), merged.remainingBalance)
    }

    @Test
    fun receivingTheSameDataTwiceChangesNothing() = runTest {
        val remote = remoteWith(layaway(payment("p1", 2_000, 0, "device-b")))
        mergeSnapshot(remote)

        val second = mergeSnapshot(remote)

        assertFalse(second.hasChanges)
        assertTrue(second.newPhotoNames.isEmpty())
    }

    @Test
    fun anOverdueLayawayIsNotReportedAsChanged() = runTest {
        val old = layaway()
        storeLocally(old.copy(state = LayawayState.Overdue))
        val lateMerge = MergeSnapshotUseCase(
            syncRepository = FakeSyncRepository(layaways, customers, products),
            merger = LayawayMerger(),
            evaluateOverdue = EvaluateOverdueUseCase(),
            scheduleReminder = scheduleReminderOf(layaways, customers, products, scheduler),
            clock = FixedClock(start + 40.days),
        )

        assertFalse(lateMerge(remoteWith(old)).hasChanges)
    }

    @Test
    fun schedulesAReminderForTheLayawaysItChanged() = runTest {
        mergeSnapshot(remoteWith(layaway(payment("p1", 2_000, 0, "device-b"))))

        assertEquals(setOf("layaway-1"), scheduler.scheduled.keys)
    }

    @Test
    fun cancelsTheReminderWhenTheOtherDeviceCompletedTheLayaway() = runTest {
        storeLocally(layaway(payment("p1", 2_000, 0, "device-a")))
        scheduler.schedule(
            OverdueReminder("layaway-1", start + 30.days, "Ana Perez", "Nike", Money(8_000)),
        )

        mergeSnapshot(remoteWith(layaway(payment("p2", 8_000, 0, "device-b"))))

        assertEquals(LayawayState.Completed, layaways.layaways.getValue("layaway-1").state)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun skipsALayawayWhoseProductDidNotArrive() = runTest {
        val remote = SyncSnapshot(listOf(customer), emptyList(), listOf(layaway()))

        val result = mergeSnapshot(remote)

        assertFalse(result.hasChanges)
        assertTrue(layaways.layaways.isEmpty())
    }
}
