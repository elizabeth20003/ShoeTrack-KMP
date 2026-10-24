package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.OverdueReminder
import com.shoetrack.domain.model.OverduePolicy
import com.shoetrack.domain.service.OverdueReminderScheduler

/** Keeps the scheduled reminder in step with the layaway: only active layaways have one. */
class ScheduleOverdueReminderUseCase(
    private val getLayawayDetail: GetLayawayDetailUseCase,
    private val scheduler: OverdueReminderScheduler,
    private val policy: OverduePolicy,
) {

    suspend operator fun invoke(layawayId: String) {
        val (layaway, customer, product) = getLayawayDetail(layawayId)

        if (layaway.state == LayawayState.Active) {
            scheduler.schedule(
                OverdueReminder(
                    layawayId = layaway.id,
                    dueAt = policy.dueAt(layaway),
                    customerName = customer.name,
                    productDescription = product.description,
                    remainingBalance = layaway.remainingBalance,
                ),
            )
        } else {
            scheduler.cancel(layaway.id)
        }
    }
}
