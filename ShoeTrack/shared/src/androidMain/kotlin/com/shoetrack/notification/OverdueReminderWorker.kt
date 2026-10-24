package com.shoetrack.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shoetrack.domain.error.LayawayNotFoundException
import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.usecase.GetLayawayDetailUseCase
import com.shoetrack.domain.usecase.RefreshOverdueUseCase
import com.shoetrack.presentation.notification.overdueNotificationText
import org.koin.mp.KoinPlatform

internal const val KEY_LAYAWAY_ID = "layaway_id"

/** Runs when a layaway reaches its due date: marks it overdue and tells the user. */
internal class OverdueReminderWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val layawayId = inputData.getString(KEY_LAYAWAY_ID) ?: return Result.failure()
        val koin = KoinPlatform.getKoin()

        koin.get<RefreshOverdueUseCase>()()
        val detail = try {
            koin.get<GetLayawayDetailUseCase>()(layawayId)
        } catch (_: LayawayNotFoundException) {
            return Result.success()
        }

        if (detail.layaway.state == LayawayState.Overdue) {
            val text = overdueNotificationText(
                customerName = detail.customer.name,
                productDescription = detail.product.description,
                remainingBalance = detail.layaway.remainingBalance,
            )
            showOverdueNotification(applicationContext, layawayId, text)
        }
        return Result.success()
    }
}
