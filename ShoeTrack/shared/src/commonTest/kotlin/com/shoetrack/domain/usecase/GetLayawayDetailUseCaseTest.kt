package com.shoetrack.domain.usecase

import com.shoetrack.domain.error.LayawayNotFoundException
import com.shoetrack.domain.fakes.FakeCustomerRepository
import com.shoetrack.domain.fakes.FakeLayawayRepository
import com.shoetrack.domain.fakes.FakeProductRepository
import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Product
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class GetLayawayDetailUseCaseTest {

    private val now = Instant.parse("2026-02-01T00:00:00Z")
    private val layaways = FakeLayawayRepository()
    private val customers = FakeCustomerRepository()
    private val products = FakeProductRepository()
    private val getDetail = GetLayawayDetailUseCase(layaways, customers, products)

    private val customer = Customer("customer-1", "Ana Perez", "3001234567", now)
    private val product = Product("product-1", "Nike Air Force 42", "", Money(100_000))
    private val layaway = Layaway(
        id = "layaway-1",
        customerId = customer.id,
        productId = product.id,
        remainingBalance = product.totalPrice,
        state = LayawayState.Active,
        createdAt = now,
        lastPaymentAt = now,
    )

    @Test
    fun combinesLayawayCustomerAndProduct() = runTest {
        layaways.layaways[layaway.id] = layaway
        customers.customers[customer.id] = customer
        products.products[product.id] = product

        val detail = getDetail(layaway.id)

        assertEquals(layaway, detail.layaway)
        assertEquals(customer, detail.customer)
        assertEquals(product, detail.product)
    }

    @Test
    fun unknownLayawayThrowsNotFound() = runTest {
        assertFailsWith<LayawayNotFoundException> { getDetail("missing") }
    }
}
