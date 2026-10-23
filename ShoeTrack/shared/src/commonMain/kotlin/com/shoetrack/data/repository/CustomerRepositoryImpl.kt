package com.shoetrack.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.shoetrack.data.mapper.toDomain
import com.shoetrack.db.ShoeTrackDatabase
import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.repository.CustomerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class CustomerRepositoryImpl(
    database: ShoeTrackDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CustomerRepository {

    private val queries = database.customerQueries

    override suspend fun save(customer: Customer) {
        withContext(dispatcher) {
            queries.upsertCustomer(
                customer.id,
                customer.name,
                customer.phone,
                customer.registeredAt.toEpochMilliseconds(),
            )
        }
    }

    override suspend fun getById(id: String): Customer? = withContext(dispatcher) {
        queries.selectCustomerById(id).executeAsOneOrNull()?.toDomain()
    }

    override fun observeAll(): Flow<List<Customer>> =
        queries.selectAllCustomers()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }
}
