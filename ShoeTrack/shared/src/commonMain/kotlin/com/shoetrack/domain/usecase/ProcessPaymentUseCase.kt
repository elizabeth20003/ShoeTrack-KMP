package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.Payment

class ProcessPaymentUseCase {

    operator fun invoke(layaway: Layaway, payment: Payment): Layaway {
        require(layaway.state !is LayawayState.Cancelled) {
            "Cannot pay a cancelled layaway"
        }
        require(layaway.state !is LayawayState.Completed) {
            "The layaway is already completed"
        }

        val newBalance = layaway.remainingBalance - payment.amount
        val newState = if (newBalance.isZeroOrLess) LayawayState.Completed else LayawayState.Active

        return layaway.copy(
            remainingBalance = newBalance,
            state = newState,
            lastPaymentAt = payment.paidAt,
            payments = layaway.payments + payment,
        )
    }
}
