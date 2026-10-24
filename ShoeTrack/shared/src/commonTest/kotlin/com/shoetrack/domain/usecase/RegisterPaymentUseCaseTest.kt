package com.shoetrack.domain.usecase

import com.shoetrack.domain.error.LayawayNotFoundException
import com.shoetrack.domain.fakes.FakeLayawayRepository
import com.shoetrack.domain.fakes.FixedClock
import com.shoetrack.domain.fakes.SequentialIdGenerator
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class RegisterPaymentUseCaseTest {

    private val now = Instant.parse("2026-02-01T00:00:00Z")
    private val repository = FakeLayawayRepository()
    private val registerPayment = RegisterPaymentUseCase(
        layawayRepository = repository,
        processPayment = ProcessPaymentUseCase(),
        idGenerator = SequentialIdGenerator(),
        clock = FixedClock(now),
        deviceId = "device-1",
    )

    private fun storedLayaway(balance: Long = 10_000): Layaway {
        val layaway = Layaway(
            id = "layaway-1",
            customerId = "customer-1",
            productId = "product-1",
            remainingBalance = Money(balance),
            state = LayawayState.Active,
            createdAt = now,
            lastPaymentAt = now,
        )
        repository.layaways[layaway.id] = layaway
        return layaway
    }

    @Test
    fun storesThePaymentAndTheUpdatedBalance() = runTest {
        storedLayaway(balance = 10_000)

        val result = registerPayment("layaway-1", Money(4_000))

        assertEquals(Money(6_000), result.remainingBalance)
        assertEquals(result, repository.layaways["layaway-1"])
        assertEquals(listOf(Money(4_000)), repository.payments.map { it.amount })
    }

    @Test
    fun stampsThePaymentWithTheDeviceAndTime() = runTest {
        storedLayaway()

        registerPayment("layaway-1", Money(1_000))

        val payment = repository.payments.single()
        assertEquals("device-1", payment.recordedByDeviceId)
        assertEquals(now, payment.paidAt)
    }

    @Test
    fun unknownLayawayThrowsNotFound() = runTest {
        assertFailsWith<LayawayNotFoundException> { registerPayment("missing", Money(1_000)) }
    }

    @Test
    fun nonPositiveAmountIsRejected() = runTest {
        storedLayaway()

        assertFailsWith<IllegalArgumentException> { registerPayment("layaway-1", Money.ZERO) }
    }
}
