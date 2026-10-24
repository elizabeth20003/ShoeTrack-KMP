package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.Layaway
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.repository.LayawayRepository
import kotlin.time.Clock

class RefreshOverdueUseCase(
    private val layawayRepository: LayawayRepository,
    private val evaluateOverdue: EvaluateOverdueUseCase,
    private val clock: Clock,
) {

    /** Returns the layaways that became overdue now. */
    suspend operator fun invoke(): List<Layaway> {
        val now = clock.now()
        val newlyOverdue = layawayRepository.getByState(LayawayState.Active)
            .map { evaluateOverdue(it, now) }
            .filter { it.state == LayawayState.Overdue }

        newlyOverdue.forEach { layawayRepository.updateState(it) }
        return newlyOverdue
    }
}
