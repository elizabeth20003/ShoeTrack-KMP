package com.shoetrack.notification

import com.shoetrack.domain.model.OverdueReminder
import com.shoetrack.domain.service.OverdueReminderScheduler
import com.shoetrack.presentation.notification.overdueNotificationText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.Clock

private const val MIN_DELAY_SECONDS = 1L

/**
 * iOS cannot run our code at a chosen time, so the notification is scheduled as a local
 * notification at the due date. The layaway itself is marked overdue the next time the app opens.
 */
class IosOverdueReminderScheduler(
    private val clock: Clock = Clock.System,
) : OverdueReminderScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun requestPermission() {
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { _, _ -> }
    }

    override fun schedule(reminder: OverdueReminder) {
        scope.launch {
            val text = overdueNotificationText(
                customerName = reminder.customerName,
                productDescription = reminder.productDescription,
                remainingBalance = reminder.remainingBalance,
            )
            val content = UNMutableNotificationContent().apply {
                setTitle(text.title)
                setBody(text.body)
                setSound(UNNotificationSound.defaultSound)
            }
            val delaySeconds = (reminder.dueAt - clock.now()).inWholeSeconds.coerceAtLeast(MIN_DELAY_SECONDS)
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                timeInterval = delaySeconds.toDouble(),
                repeats = false,
            )
            // Reusing the layaway id as identifier makes a new request replace the pending one.
            center.addNotificationRequest(
                UNNotificationRequest.requestWithIdentifier(reminder.layawayId, content, trigger),
                withCompletionHandler = null,
            )
        }
    }

    override fun cancel(layawayId: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(layawayId))
    }
}
