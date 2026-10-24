package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import kotlin.time.Instant

class EvaluateOverdueUseCase(
    private val overdueAfterDays: Int = 30,
) {

    operator fun invoke(layaway: Layaway, now: Instant): Layaway {
        if (layaway.state != LayawayState.Active) return layaway

        val daysWithoutPayment = (now - layaway.lastPaymentAt).inWholeDays
        return if (daysWithoutPayment >= overdueAfterDays) {
            layaway.copy(state = LayawayState.Overdue)
        } else {
            layaway
        }
    }
}
