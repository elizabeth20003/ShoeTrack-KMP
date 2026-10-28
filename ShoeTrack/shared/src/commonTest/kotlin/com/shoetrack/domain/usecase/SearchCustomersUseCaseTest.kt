package com.shoetrack.domain.usecase

import com.shoetrack.domain.fakes.FakeCustomerRepository
import com.shoetrack.domain.model.Customer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SearchCustomersUseCaseTest {

    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val customers = FakeCustomerRepository()
    private val searchCustomers = SearchCustomersUseCase(customers)

    private fun register(id: String, name: String, phone: String = "3001234567") {
        customers.customers[id] = Customer(id, name, phone, now)
    }

    @Test
    fun findsCustomersByPartOfTheNameIgnoringCaseAndAccents() = runTest {
        register("1", "María Pérez")
        register("2", "Luis Gomez")

        assertEquals(listOf("1"), searchCustomers("maria").map { it.id })
    }

    @Test
    fun aVeryShortQueryReturnsNothing() = runTest {
        register("1", "Ana Perez")

        assertEquals(emptyList(), searchCustomers("a"))
    }

    @Test
    fun theSameCustomerRegisteredTwiceIsListedOnce() = runTest {
        register("1", "Ana Perez")
        register("2", "ana perez ")

        assertEquals(1, searchCustomers("ana").size)
    }

    @Test
    fun returnsAtMostThreeCustomers() = runTest {
        repeat(5) { register("$it", "Ana $it", phone = "300000000$it") }

        assertEquals(3, searchCustomers("ana").size)
    }
}
