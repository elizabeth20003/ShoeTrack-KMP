package com.shoetrack.domain.usecase

import com.shoetrack.domain.fakes.FakeCustomerRepository
import com.shoetrack.domain.fakes.FakeLayawayRepository
import com.shoetrack.domain.fakes.FakeOverdueReminderScheduler
import com.shoetrack.domain.fakes.FakeProductRepository
import com.shoetrack.domain.fakes.scheduleReminderOf
import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Product
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class ScheduleOverdueReminderUseCaseTest {

    private val now = Instant.parse("2026-02-01T00:00:00Z")
    private val layaways = FakeLayawayRepository()
    private val customers = FakeCustomerRepository()
    private val products = FakeProductRepository()
    private val scheduler = FakeOverdueReminderScheduler()
    private val scheduleReminder = scheduleReminderOf(layaways, customers, products, scheduler)

    private fun store(state: LayawayState) {
        customers.customers["customer-1"] = Customer("customer-1", "Ana Perez", "3001234567", now)
        products.products["product-1"] = Product("product-1", "Nike Air Force 42", "", Money(10_000))
        layaways.layaways["layaway-1"] = Layaway(
            id = "layaway-1",
            customerId = "customer-1",
            productId = "product-1",
            remainingBalance = Money(10_000),
            state = state,
            createdAt = now,
            lastPaymentAt = now,
        )
    }

    @Test
    fun activeLayawayGetsAReminderWithItsDetails() = runTest {
        store(LayawayState.Active)

        scheduleReminder("layaway-1")

        val reminder = scheduler.scheduled.getValue("layaway-1")
        assertEquals(now + 30.days, reminder.dueAt)
        assertEquals("Ana Perez", reminder.customerName)
        assertEquals("Nike Air Force 42", reminder.productDescription)
        assertEquals(Money(10_000), reminder.remainingBalance)
    }

    @Test
    fun layawayThatIsNotActiveHasItsReminderCancelled() = runTest {
        store(LayawayState.Active)
        scheduleReminder("layaway-1")
        layaways.layaways["layaway-1"] = layaways.layaways.getValue("layaway-1").copy(state = LayawayState.Cancelled)

        scheduleReminder("layaway-1")

        assertNull(scheduler.scheduled["layaway-1"])
    }
}
