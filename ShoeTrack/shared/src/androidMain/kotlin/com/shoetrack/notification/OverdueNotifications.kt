package com.shoetrack.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shoetrack.presentation.notification.NotificationText
import org.jetbrains.compose.resources.getString
import shoetrack.shared.generated.resources.Res
import shoetrack.shared.generated.resources.notification_channel_name

private const val CHANNEL_ID = "overdue_layaways"

internal suspend fun showOverdueNotification(context: Context, layawayId: String, text: NotificationText) {
    val manager = NotificationManagerCompat.from(context)
    if (!manager.areNotificationsEnabled()) return

    createChannel(context)
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(context.applicationInfo.icon)
        .setContentTitle(text.title)
        .setContentText(text.body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
        .setContentIntent(openLayawayIntent(context, layawayId))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    @Suppress("MissingPermission") // areNotificationsEnabled() was checked above
    manager.notify(layawayId.hashCode(), notification)
}

private suspend fun createChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        CHANNEL_ID,
        getString(Res.string.notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    )
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

private fun openLayawayIntent(context: Context, layawayId: String): PendingIntent {
    val launchIntent = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName)) {
        "The app has no launcher activity"
    }.apply {
        putExtra(NotificationIntents.EXTRA_LAYAWAY_ID, layawayId)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    return PendingIntent.getActivity(
        context,
        layawayId.hashCode(),
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
