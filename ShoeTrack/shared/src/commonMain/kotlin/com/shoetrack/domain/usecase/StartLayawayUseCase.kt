package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.Customer
import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.Money
import com.shoetrack.domain.model.Product
import com.shoetrack.domain.repository.CustomerRepository
import com.shoetrack.domain.repository.LayawayRepository
import com.shoetrack.domain.repository.ProductRepository
import com.shoetrack.domain.service.IdGenerator
import com.shoetrack.domain.validation.PhoneNumberRules
import kotlin.time.Clock

data class NewLayaway(
    val customerName: String,
    val customerPhone: String,
    val productDescription: String,
    val productPhotoPath: String,
    val productPrice: Money,
    val initialPayment: Money = Money.ZERO,
)

class StartLayawayUseCase(
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val layawayRepository: LayawayRepository,
    private val createLayaway: CreateLayawayUseCase,
    private val registerPayment: RegisterPaymentUseCase,
    private val scheduleReminder: ScheduleOverdueReminderUseCase,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {

    suspend operator fun invoke(request: NewLayaway): Layaway {
        require(request.customerName.isNotBlank()) { "The customer name is required" }
        require(request.productDescription.isNotBlank()) { "The product description is required" }
        require(PhoneNumberRules.isValid(request.customerPhone)) { "The phone number is not valid" }
        require(request.initialPayment <= request.productPrice) {
            "The initial payment cannot be more than the price"
        }
        require(request.initialPayment >= Money.ZERO) { "The initial payment cannot be negative" }

        val now = clock.now()
        val customer = Customer(
            id = idGenerator.newId(),
            name = request.customerName.trim(),
            phone = PhoneNumberRules.normalize(request.customerPhone),
            registeredAt = now,
        )
        val product = Product(
            id = idGenerator.newId(),
            description = request.productDescription.trim(),
            photoPath = request.productPhotoPath,
            totalPrice = request.productPrice,
        )
        val layaway = createLayaway(idGenerator.newId(), customer.id, product, now)

        customerRepository.save(customer)
        productRepository.save(product)
        layawayRepository.create(layaway)
        scheduleReminder(layaway.id)

        return if (request.initialPayment.isPositive) {
            registerPayment(layaway.id, request.initialPayment)
        } else {
            layaway
        }
    }
}
