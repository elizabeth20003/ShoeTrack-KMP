package com.shoetrack.domain.usecase

import com.shoetrack.domain.error.LayawayNotFoundException
import com.shoetrack.domain.model.LayawayDetail
import com.shoetrack.domain.repository.CustomerRepository
import com.shoetrack.domain.repository.LayawayRepository
import com.shoetrack.domain.repository.ProductRepository

class GetLayawayDetailUseCase(
    private val layawayRepository: LayawayRepository,
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
) {

    suspend operator fun invoke(id: String): LayawayDetail {
        val layaway = layawayRepository.getById(id) ?: throw LayawayNotFoundException(id)
        val customer = checkNotNull(customerRepository.getById(layaway.customerId)) {
            "Layaway $id references a missing customer"
        }
        val product = checkNotNull(productRepository.getById(layaway.productId)) {
            "Layaway $id references a missing product"
        }
        return LayawayDetail(layaway, customer, product)
    }
}
