package com.shoetrack.data.sync

import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Payment
import com.shoetrack.domain.model.Product
import com.shoetrack.domain.model.SyncSnapshot
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SyncMessagesTest {

    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val snapshot = SyncSnapshot(
        customers = listOf(Customer("customer-1", "Ana Perez", "3001234567", now)),
        products = listOf(Product("product-1", "Nike Air Force 42", "photo-1.jpg", Money(10_000))),
        layaways = listOf(
            Layaway(
                id = "layaway-1",
                customerId = "customer-1",
                productId = "product-1",
                remainingBalance = Money(6_000),
                state = LayawayState.Active,
                createdAt = now,
                lastPaymentAt = now,
                payments = listOf(Payment("payment-1", "layaway-1", Money(4_000), now, "device-1")),
            ),
        ),
    )

    @Test
    fun aSnapshotSurvivesTheTripOverTheWire() {
        val text = Json.encodeToString(SyncMessage.serializer(), SnapshotMessage(snapshot.toDto()))

        val received = Json.decodeFromString(SyncMessage.serializer(), text) as SnapshotMessage

        assertEquals(snapshot, received.snapshot.toDomain())
    }

    @Test
    fun everyMessageTypeCanBeDecoded() {
        val messages = listOf(PhotoRequestMessage("photo-1.jpg"), PhotoMessage("photo-1.jpg", null), DoneMessage)

        for (message in messages) {
            val text = Json.encodeToString(SyncMessage.serializer(), message)
            assertEquals(message, Json.decodeFromString(SyncMessage.serializer(), text))
        }
    }
}
