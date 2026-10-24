package com.shoetrack.domain.usecase

import com.shoetrack.domain.fakes.FakeLayawayRepository
import com.shoetrack.domain.fakes.FixedClock
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class RefreshOverdueUseCaseTest {

    private val today = Instant.parse("2026-03-01T00:00:00Z")
    private val repository = FakeLayawayRepository()
    private val refreshOverdue = RefreshOverdueUseCase(
        layawayRepository = repository,
        evaluateOverdue = EvaluateOverdueUseCase(overdueAfterDays = 30),
        clock = FixedClock(today),
    )

    private fun store(id: String, lastPaymentAt: Instant, state: LayawayState = LayawayState.Active) {
        repository.layaways[id] = Layaway(
            id = id,
            customerId = "customer-1",
            productId = "product-1",
            remainingBalance = Money(10_000),
            state = state,
            createdAt = lastPaymentAt,
            lastPaymentAt = lastPaymentAt,
        )
    }

    @Test
    fun marksOnlyTheLayawaysPastTheLimit() = runTest {
        store("old", today - 40.days)
        store("recent", today - 5.days)

        val changed = refreshOverdue()

        assertEquals(1, changed)
        assertEquals(LayawayState.Overdue, repository.layaways["old"]?.state)
        assertEquals(LayawayState.Active, repository.layaways["recent"]?.state)
    }

    @Test
    fun ignoresLayawaysThatAreNotActive() = runTest {
        store("done", today - 90.days, LayawayState.Completed)

        assertEquals(0, refreshOverdue())
        assertEquals(LayawayState.Completed, repository.layaways["done"]?.state)
    }
}
