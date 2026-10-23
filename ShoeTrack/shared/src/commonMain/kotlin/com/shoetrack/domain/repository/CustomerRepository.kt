package com.shoetrack.domain.repository

import com.shoetrack.domain.model.Customer
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {

    /** Inserts the customer, or replaces the one with the same id. */
    suspend fun save(customer: Customer)

    suspend fun getById(id: String): Customer?

    /** Emits all customers, ordered by name, whenever the list changes. */
    fun observeAll(): Flow<List<Customer>>
}
