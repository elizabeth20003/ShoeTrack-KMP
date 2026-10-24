package com.shoetrack.domain.usecase

import com.shoetrack.domain.error.LayawayNotFoundException
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Payment
import com.shoetrack.domain.repository.LayawayRepository
import com.shoetrack.domain.service.IdGenerator
import kotlin.time.Clock

class RegisterPaymentUseCase(
    private val layawayRepository: LayawayRepository,
    private val processPayment: ProcessPaymentUseCase,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val deviceId: String,
) {

    suspend operator fun invoke(layawayId: String, amount: Money): Layaway {
        require(amount.isPositive) { "The payment amount must be greater than zero" }

        val layaway = layawayRepository.getById(layawayId)
            ?: throw LayawayNotFoundException(layawayId)
        val payment = Payment(
            id = idGenerator.newId(),
            layawayId = layaway.id,
            amount = amount,
            paidAt = clock.now(),
            recordedByDeviceId = deviceId,
        )

        val updated = processPayment(layaway, payment)
        layawayRepository.registerPayment(updated, payment)
        return updated
    }
}
