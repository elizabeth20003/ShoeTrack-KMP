package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Product
import kotlin.time.Instant

class CreateLayawayUseCase {

    operator fun invoke(
        id: String,
        customerId: String,
        product: Product,
        now: Instant,
    ): Layaway {
        require(product.totalPrice.isPositive) { "The product price must be greater than zero" }

        return Layaway(
            id = id,
            customerId = customerId,
            productId = product.id,
            remainingBalance = product.totalPrice,
            state = LayawayState.Active,
            createdAt = now,
            lastPaymentAt = now,
        )
    }
}
