package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.repository.LayawayRepository
import kotlin.time.Clock

class RefreshOverdueUseCase(
    private val layawayRepository: LayawayRepository,
    private val evaluateOverdue: EvaluateOverdueUseCase,
    private val clock: Clock,
) {

    /** Returns how many layaways became overdue. */
    suspend operator fun invoke(): Int {
        val now = clock.now()
        val newlyOverdue = layawayRepository.getByState(LayawayState.Active)
            .map { evaluateOverdue(it, now) }
            .filter { it.state == LayawayState.Overdue }

        newlyOverdue.forEach { layawayRepository.updateState(it) }
        return newlyOverdue.size
    }
}
