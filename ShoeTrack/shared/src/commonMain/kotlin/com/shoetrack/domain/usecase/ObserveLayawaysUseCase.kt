package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.LayawaySummary
import com.shoetrack.domain.repository.LayawayRepository
import kotlinx.coroutines.flow.Flow

class ObserveLayawaysUseCase(private val layawayRepository: LayawayRepository) {

    operator fun invoke(): Flow<List<LayawaySummary>> = layawayRepository.observeSummaries()
}
