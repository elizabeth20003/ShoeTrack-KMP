package com.shoetrack.presentation.notification

import com.shoetrack.domain.model.Money
import com.shoetrack.presentation.format.format
import org.jetbrains.compose.resources.getString
import shoetrack.shared.generated.resources.Res
import shoetrack.shared.generated.resources.notification_overdue_body
import shoetrack.shared.generated.resources.notification_overdue_title

data class NotificationText(val title: String, val body: String)

suspend fun overdueNotificationText(
    customerName: String,
    productDescription: String,
    remainingBalance: Money,
): NotificationText = NotificationText(
    title = getString(Res.string.notification_overdue_title),
    body = getString(
        Res.string.notification_overdue_body,
        customerName,
        productDescription,
        remainingBalance.format(),
    ),
)
