package com.shoetrack.presentation

import com.shoetrack.domain.fakes.FakeLayawayRepository
import com.shoetrack.domain.fakes.FixedClock
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.usecase.EvaluateOverdueUseCase
import com.shoetrack.domain.usecase.ObserveLayawaysUseCase
import com.shoetrack.domain.usecase.RefreshOverdueUseCase
import com.shoetrack.presentation.list.LayawayListUiState
import com.shoetrack.presentation.list.LayawayListViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class LayawayListViewModelTest : MainDispatcherTest() {

    private val today = Instant.parse("2026-03-01T00:00:00Z")
    private val layaways = FakeLayawayRepository()

    private fun viewModel() = LayawayListViewModel(
        observeLayaways = ObserveLayawaysUseCase(layaways),
        refreshOverdue = RefreshOverdueUseCase(layaways, EvaluateOverdueUseCase(), FixedClock(today)),
    )

    private fun store(id: String, lastPaymentAt: Instant) {
        layaways.layaways[id] = Layaway(
            id = id,
            customerId = "customer-1",
            productId = "product-1",
            remainingBalance = Money(10_000),
            state = LayawayState.Active,
            createdAt = lastPaymentAt,
            lastPaymentAt = lastPaymentAt,
        )
    }

    @Test
    fun showsTheStoredLayaways() = runTest {
        store("layaway-1", today)

        val state = viewModel().uiState.first { it !is LayawayListUiState.Loading }

        assertEquals(1, assertIs<LayawayListUiState.Loaded>(state).layaways.size)
    }

    @Test
    fun showsAnErrorWhenObservingFails() = runTest {
        layaways.observeFailure = IllegalStateException("database closed")

        val state = viewModel().uiState.first { it !is LayawayListUiState.Loading }

        assertIs<LayawayListUiState.Error>(state)
    }

    @Test
    fun marksStaleLayawaysAsOverdueWhenOpened() {
        store("stale", today - 45.days)

        viewModel()

        assertEquals(LayawayState.Overdue, layaways.layaways["stale"]?.state)
    }
}
