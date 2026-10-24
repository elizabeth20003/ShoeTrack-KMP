package com.shoetrack.domain.service

import com.shoetrack.domain.model.OverdueReminder

/** Schedules the device notification for a layaway; scheduling again replaces the earlier one. */
interface OverdueReminderScheduler {
    fun schedule(reminder: OverdueReminder)

    fun cancel(layawayId: String)
}
