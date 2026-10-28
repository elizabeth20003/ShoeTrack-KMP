package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.repository.CustomerRepository
import com.shoetrack.domain.search.matchesAllWords
import kotlinx.coroutines.flow.first

/** Finds registered customers by name, so a returning customer is not entered twice. */
class SearchCustomersUseCase(private val customerRepository: CustomerRepository) {

    suspend operator fun invoke(query: String, limit: Int = MAX_RESULTS): List<Customer> {
        if (query.trim().length < MIN_QUERY_LENGTH) return emptyList()

        return customerRepository.observeAll().first()
            .filter { matchesAllWords(query, it.name) }
            .distinctBy { it.name.trim().lowercase() to it.phone }
            .take(limit)
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val MAX_RESULTS = 3
    }
}
