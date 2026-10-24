package com.shoetrack.domain.usecase

import com.shoetrack.domain.fakes.FakeCustomerRepository
import com.shoetrack.domain.fakes.FakeLayawayRepository
import com.shoetrack.domain.fakes.FakeProductRepository
import com.shoetrack.domain.fakes.FixedClock
import com.shoetrack.domain.fakes.SequentialIdGenerator
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class StartLayawayUseCaseTest {

    private val now = Instant.parse("2026-02-01T00:00:00Z")
    private val customers = FakeCustomerRepository()
    private val products = FakeProductRepository()
    private val layaways = FakeLayawayRepository()
    private val idGenerator = SequentialIdGenerator()
    private val clock = FixedClock(now)
    private val startLayaway = StartLayawayUseCase(
        customerRepository = customers,
        productRepository = products,
        layawayRepository = layaways,
        createLayaway = CreateLayawayUseCase(),
        registerPayment = RegisterPaymentUseCase(
            layawayRepository = layaways,
            processPayment = ProcessPaymentUseCase(),
            idGenerator = idGenerator,
            clock = clock,
            deviceId = "device-1",
        ),
        idGenerator = idGenerator,
        clock = clock,
    )

    private fun request(initialPayment: Money = Money.ZERO) = NewLayaway(
        customerName = "  Ana Perez ",
        customerPhone = "3001234567",
        productDescription = "Nike Air Force 42",
        productPhotoPath = "photos/shoe.jpg",
        productPrice = Money(100_000),
        initialPayment = initialPayment,
    )

    @Test
    fun savesCustomerProductAndLayaway() = runTest {
        val result = startLayaway(request())

        assertEquals("Ana Perez", customers.customers.values.single().name)
        assertEquals("Nike Air Force 42", products.products.values.single().description)
        assertEquals(result, layaways.layaways.values.single())
        assertEquals(Money(100_000), result.remainingBalance)
    }

    @Test
    fun appliesTheInitialPaymentWhenGiven() = runTest {
        val result = startLayaway(request(initialPayment = Money(30_000)))

        assertEquals(Money(70_000), result.remainingBalance)
        assertEquals(1, layaways.payments.size)
    }

    @Test
    fun fullInitialPaymentCompletesTheLayawayImmediately() = runTest {
        val result = startLayaway(request(initialPayment = Money(100_000)))

        assertEquals(LayawayState.Completed, result.state)
    }

    @Test
    fun storesOnlyTheDigitsOfThePhoneNumber() = runTest {
        startLayaway(request().copy(customerPhone = "300 123 4567"))

        assertEquals("3001234567", customers.customers.values.single().phone)
    }

    @Test
    fun invalidPhoneNumberIsRejectedBeforeSavingAnything() = runTest {
        assertFailsWith<IllegalArgumentException> {
            startLayaway(request().copy(customerPhone = "12345"))
        }

        assertEquals(0, customers.customers.size)
    }

    @Test
    fun blankCustomerNameIsRejectedBeforeSavingAnything() = runTest {
        assertFailsWith<IllegalArgumentException> {
            startLayaway(request().copy(customerName = " "))
        }

        assertEquals(0, customers.customers.size)
    }
}
