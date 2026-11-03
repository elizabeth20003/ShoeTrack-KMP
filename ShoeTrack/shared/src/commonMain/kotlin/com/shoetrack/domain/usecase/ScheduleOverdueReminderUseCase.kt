package com.shoetrack.domain.usecase

import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.OverdueReminder
import com.shoetrack.domain.model.OverduePolicy
import com.shoetrack.domain.service.OverdueReminderScheduler
import kotlin.time.Clock

/** Keeps the scheduled reminder in step with the layaway: only active layaways have one. */
class ScheduleOverdueReminderUseCase(
    private val getLayawayDetail: GetLayawayDetailUseCase,
    private val scheduler: OverdueReminderScheduler,
    private val policy: OverduePolicy,
    private val clock: Clock,
) {

    suspend operator fun invoke(layawayId: String) {
        val (layaway, customer, product) = getLayawayDetail(layawayId)

        // A layaway already past its due date is marked overdue on the next refresh, not by a late reminder.
        if (layaway.state == LayawayState.Active && policy.dueAt(layaway) > clock.now()) {
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
