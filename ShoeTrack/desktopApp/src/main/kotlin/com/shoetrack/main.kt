package com.shoetrack

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.shoetrack.data.local.JvmDatabaseDriverFactory
import com.shoetrack.di.initKoin
import com.shoetrack.notification.NoOpOverdueReminderScheduler
import java.io.File

fun main() {
    val appDirectory = File(System.getProperty("user.home"), ".shoetrack")
    initKoin(
        driverFactory = JvmDatabaseDriverFactory(appDirectory),
        reminderScheduler = NoOpOverdueReminderScheduler,
        photosDirectory = File(appDirectory, "photos").path,
        deviceId = DESKTOP_DEVICE_ID,
    )
    runApplication()
}

private const val DESKTOP_DEVICE_ID = "desktop-dev"

private fun runApplication() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ShoeTrack",
    ) {
        App()
    }
}