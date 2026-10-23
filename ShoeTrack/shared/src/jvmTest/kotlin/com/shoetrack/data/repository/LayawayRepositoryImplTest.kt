package com.shoetrack.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Payment
import com.shoetrack.domain.model.Product
import com.shoetrack.domain.usecase.CreateLayawayUseCase
import com.shoetrack.domain.usecase.ProcessPaymentUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class LayawayRepositoryImplTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        ShoeTrackDatabase.Schema.create(it)
    }
    private val repository = LayawayRepositoryImpl(
        database = ShoeTrackDatabase(driver),
        deviceId = "test-device",
        dispatcher = Dispatchers.Unconfined,
    )

    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val product = Product(
        id = "product-1",
        description = "Nike Air Force 42",
        photoPath = "photos/product-1.jpg",
        totalPrice = Money(10_000),
    )

    @AfterTest
    fun closeDatabase() {
        driver.close()
    }

    private fun newLayaway(id: String = "layaway-1"): Layaway =
        CreateLayawayUseCase()(id, customerId = "customer-1", product = product, now = now)

    private fun payment(amount: Long) =
        Payment("payment-1", "layaway-1", Money(amount), now, "test-device")

    @Test
    fun createdLayawayCanBeReadBack() = runBlocking {
        val layaway = newLayaway()
        repository.create(layaway)

        assertEquals(layaway, repository.getById("layaway-1"))
    }

    @Test
    fun unknownLayawayIsNull() = runBlocking {
        assertNull(repository.getById("missing"))
    }

    @Test
    fun registeredPaymentUpdatesBalanceStateAndHistory() = runBlocking {
        repository.create(newLayaway())
        val payment = payment(4_000)
        val updated = ProcessPaymentUseCase()(newLayaway(), payment)

        repository.registerPayment(updated, payment)
        val stored = repository.getById("layaway-1")

        assertEquals(Money(6_000), stored?.remainingBalance)
        assertEquals(LayawayState.Active, stored?.state)
        assertEquals(listOf(payment), stored?.payments)
    }

    @Test
    fun paymentCoveringTheBalanceCompletesTheStoredLayaway() = runBlocking {
        repository.create(newLayaway())
        val payment = payment(10_000)
        val updated = ProcessPaymentUseCase()(newLayaway(), payment)

        repository.registerPayment(updated, payment)

        assertEquals(LayawayState.Completed, repository.getById("layaway-1")?.state)
    }

    @Test
    fun getByStateReturnsOnlyMatchingLayaways() = runBlocking {
        repository.create(newLayaway("layaway-1"))
        repository.create(newLayaway("layaway-2"))
        repository.updateState(newLayaway("layaway-2").copy(state = LayawayState.Overdue))

        val overdue = repository.getByState(LayawayState.Overdue)
        val active = repository.getByState(LayawayState.Active)

        assertEquals(listOf("layaway-2"), overdue.map { it.id })
        assertEquals(listOf("layaway-1"), active.map { it.id })
    }

    @Test
    fun newLayawaysArePendingSync() = runBlocking {
        repository.create(newLayaway())

        assertTrue(repository.getPendingSync().any { it.id == "layaway-1" })
    }
}
