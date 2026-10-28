package com.shoetrack.data.sync

import com.shoetrack.domain.fakes.FakeCustomerRepository
import com.shoetrack.domain.fakes.FakeLayawayRepository
import com.shoetrack.domain.fakes.FakeOverdueReminderScheduler
import com.shoetrack.domain.fakes.FakePhotoStorage
import com.shoetrack.domain.fakes.FakeProductRepository
import com.shoetrack.domain.fakes.FakeSyncRepository
import com.shoetrack.domain.fakes.FixedClock
import com.shoetrack.domain.fakes.scheduleReminderOf
import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Payment
import com.shoetrack.domain.model.Product
import com.shoetrack.domain.sync.LayawayMerger
import com.shoetrack.domain.usecase.EvaluateOverdueUseCase
import com.shoetrack.domain.usecase.MergeSnapshotUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class SyncSessionTest {

    private val start = Instant.parse("2026-01-01T00:00:00Z")
    private val customer = Customer("customer-1", "Ana Perez", "3001234567", start)
    private val product = Product("product-1", "Nike Air Force 42", "photo-1.jpg", Money(10_000))

    private class Device {
        val layaways = FakeLayawayRepository()
        val customers = FakeCustomerRepository()
        val products = FakeProductRepository()
        val photos = FakePhotoStorage()
        private val syncRepository = FakeSyncRepository(layaways, customers, products)
        val session = SyncSession(
            syncRepository = syncRepository,
            mergeSnapshot = MergeSnapshotUseCase(
                syncRepository = syncRepository,
                merger = LayawayMerger(),
                evaluateOverdue = EvaluateOverdueUseCase(),
                scheduleReminder = scheduleReminderOf(layaways, customers, products, FakeOverdueReminderScheduler()),
                clock = FixedClock(Instant.parse("2026-01-02T00:00:00Z")),
            ),
            photoStorage = photos,
        )
    }

    private class InMemoryChannel(
        private val incoming: Channel<SyncMessage>,
        private val outgoing: Channel<SyncMessage>,
    ) : MessageChannel {
        override suspend fun send(message: SyncMessage) = outgoing.send(message)
        override suspend fun receive(): SyncMessage = incoming.receive()
    }

    private fun connectedChannels(): Pair<MessageChannel, MessageChannel> {
        val toResponder = Channel<SyncMessage>(Channel.UNLIMITED)
        val toInitiator = Channel<SyncMessage>(Channel.UNLIMITED)
        return InMemoryChannel(toInitiator, toResponder) to InMemoryChannel(toResponder, toInitiator)
    }

    private suspend fun sync(initiator: Device, responder: Device) = coroutineScope {
        val (initiatorChannel, responderChannel) = connectedChannels()
        val serving = async { responder.session.runAsResponder(responderChannel) }
        initiator.session.runAsInitiator(initiatorChannel)
        serving.await()
    }

    private fun payment(id: String, amount: Long, device: String) =
        Payment(id, "layaway-1", Money(amount), start + 1.days, device)

    private fun Device.store(vararg payments: Payment, withPhoto: Boolean = false) {
        customers.customers[customer.id] = customer
        products.products[product.id] = product
        val paid = payments.fold(Money.ZERO) { total, payment -> total + payment.amount }
        layaways.layaways["layaway-1"] = Layaway(
            id = "layaway-1",
            customerId = customer.id,
            productId = product.id,
            remainingBalance = product.totalPrice - paid,
            state = LayawayState.Active,
            createdAt = start,
            lastPaymentAt = payments.maxOfOrNull { it.paidAt } ?: start,
            payments = payments.toList(),
        )
        if (withPhoto) photos.photos["photo-1.jpg"] = byteArrayOf(1, 2, 3)
    }

    @Test
    fun anEmptyDeviceReceivesTheLayawayAndItsPhoto() = runTest {
        val shop = Device().apply { store(payment("p1", 2_000, "shop"), withPhoto = true) }
        val newPhone = Device()

        sync(initiator = newPhone, responder = shop)

        assertEquals(Money(8_000), newPhone.layaways.layaways.getValue("layaway-1").remainingBalance)
        assertContentEquals(byteArrayOf(1, 2, 3), newPhone.photos.photos["photo-1.jpg"])
    }

    @Test
    fun bothDevicesEndWithPaymentsRecordedOnEitherOne() = runTest {
        val cashier = Device().apply { store(payment("p1", 2_000, "cashier")) }
        val owner = Device().apply { store(payment("p2", 3_000, "owner")) }

        sync(initiator = cashier, responder = owner)

        for (device in listOf(cashier, owner)) {
            val layaway = device.layaways.layaways.getValue("layaway-1")
            assertEquals(setOf("p1", "p2"), layaway.payments.map { it.id }.toSet())
            assertEquals(Money(5_000), layaway.remainingBalance)
        }
    }

    @Test
    fun theResponderReceivesTheInitiatorsPhotosToo() = runTest {
        val phone = Device().apply { store(withPhoto = true) }
        val desktop = Device()

        sync(initiator = phone, responder = desktop)

        assertContentEquals(byteArrayOf(1, 2, 3), desktop.photos.photos["photo-1.jpg"])
    }

    @Test
    fun aPhotoNameThatCouldEscapeTheFolderIsNeverRequested() = runTest {
        val shop = Device().apply {
            store()
            products.products[product.id] = product.copy(photoPath = "../secret.jpg")
        }
        val phone = Device()

        sync(initiator = phone, responder = shop)

        assertNull(phone.photos.photos["../secret.jpg"])
    }

    @Test
    fun syncingTwiceLeavesTheSameResult() = runTest {
        val a = Device().apply { store(payment("p1", 2_000, "a")) }
        val b = Device().apply { store(payment("p2", 3_000, "b")) }
        sync(a, b)
        val afterFirst = a.layaways.layaways.getValue("layaway-1")

        sync(a, b)

        assertEquals(afterFirst, a.layaways.layaways.getValue("layaway-1"))
        assertEquals(afterFirst, b.layaways.layaways.getValue("layaway-1"))
    }
}
