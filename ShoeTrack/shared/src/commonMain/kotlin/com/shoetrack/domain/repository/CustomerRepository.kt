package com.shoetrack.domain.repository

import com.shoetrack.domain.model.Customer
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {

    suspend fun save(customer: Customer)

    suspend fun getById(id: String): Customer?

    fun observeAll(): Flow<List<Customer>>
}
