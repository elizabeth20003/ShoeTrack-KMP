package com.shoetrack.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.shoetrack.domain.model.OverdueReminder
import com.shoetrack.domain.service.OverdueReminderScheduler
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration

/** Uses WorkManager, which keeps pending work across app restarts and device reboots. */
class AndroidOverdueReminderScheduler(
    context: Context,
    private val clock: Clock = Clock.System,
) : OverdueReminderScheduler {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun schedule(reminder: OverdueReminder) {
        val delay = (reminder.dueAt - clock.now()).coerceAtLeast(Duration.ZERO)
        val request = OneTimeWorkRequestBuilder<OverdueReminderWorker>()
            .setInitialDelay(delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_LAYAWAY_ID to reminder.layawayId))
            .build()
        workManager.enqueueUniqueWork(workName(reminder.layawayId), ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(layawayId: String) {
        workManager.cancelUniqueWork(workName(layawayId))
    }

    private fun workName(layawayId: String) = "overdue-reminder-$layawayId"
}
