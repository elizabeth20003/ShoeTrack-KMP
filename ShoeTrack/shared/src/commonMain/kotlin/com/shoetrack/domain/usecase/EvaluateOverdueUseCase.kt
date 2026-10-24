package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.OverduePolicy
import kotlin.time.Instant

class EvaluateOverdueUseCase(
    private val policy: OverduePolicy = OverduePolicy(),
) {

    operator fun invoke(layaway: Layaway, now: Instant): Layaway =
        if (layaway.state == LayawayState.Active && policy.isOverdue(layaway, now)) {
            layaway.copy(state = LayawayState.Overdue)
        } else {
            layaway
        }
}
