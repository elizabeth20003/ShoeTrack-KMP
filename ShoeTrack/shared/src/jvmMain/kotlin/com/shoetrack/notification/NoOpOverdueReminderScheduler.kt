package com.shoetrack.notification

import com.shoetrack.domain.model.OverdueReminder
import com.shoetrack.domain.service.OverdueReminderScheduler

/** The desktop build has no background notifications. */
object NoOpOverdueReminderScheduler : OverdueReminderScheduler {
    override fun schedule(reminder: OverdueReminder) = Unit

    override fun cancel(layawayId: String) = Unit
}
