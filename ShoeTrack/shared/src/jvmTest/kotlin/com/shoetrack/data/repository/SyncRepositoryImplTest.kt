package com.shoetrack.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Payment
import com.shoetrack.domain.model.Product
import com.shoetrack.domain.model.SyncSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SyncRepositoryImplTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        ShoeTrackDatabase.Schema.create(it)
    }
    private val repository = SyncRepositoryImpl(
        database = ShoeTrackDatabase(driver),
        deviceId = "test-device",
        dispatcher = Dispatchers.Unconfined,
    )

    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val customer = Customer("customer-1", "Ana Perez", "3001234567", now)
    private val product = Product("product-1", "Nike Air Force 42", "photo-1.jpg", Money(10_000))
    private val payment = Payment("payment-1", "layaway-1", Money(4_000), now, "other-device")
    private val layaway = Layaway(
        id = "layaway-1",
        customerId = customer.id,
        productId = product.id,
        remainingBalance = Money(6_000),
        state = LayawayState.Active,
        createdAt = now,
        lastPaymentAt = now,
        payments = listOf(payment),
    )
    private val snapshot = SyncSnapshot(listOf(customer), listOf(product), listOf(layaway))

    @AfterTest
    fun closeDatabase() {
        driver.close()
    }

    @Test
    fun storedDataComesBackInTheSnapshot() = runBlocking {
        repository.store(snapshot)

        assertEquals(snapshot, repository.snapshot())
    }

    @Test
    fun storingTheSameDataTwiceDoesNotDuplicateAnything() = runBlocking {
        repository.store(snapshot)
        repository.store(snapshot)

        assertEquals(snapshot, repository.snapshot())
    }

    @Test
    fun aStoredLayawayIsReplacedByTheNewVersion() = runBlocking {
        repository.store(snapshot)
        val completed = layaway.copy(remainingBalance = Money.ZERO, state = LayawayState.Completed)

        repository.store(SyncSnapshot(layaways = listOf(completed)))

        assertEquals(LayawayState.Completed, repository.snapshot().layaways.single().state)
    }
}
