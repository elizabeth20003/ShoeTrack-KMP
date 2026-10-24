package com.shoetrack

import com.shoetrack.data.local.IosDatabaseDriverFactory
import com.shoetrack.di.initKoin
import com.shoetrack.notification.IosOverdueReminderScheduler
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask

private const val DEVICE_ID_KEY = "shoetrack_device_id"

/** Entry point called from Swift at launch; mirrors what the Android Application class does. */
fun initKoinForIos() {
    val reminderScheduler = IosOverdueReminderScheduler().also { it.requestPermission() }
    initKoin(
        driverFactory = IosDatabaseDriverFactory(),
        reminderScheduler = reminderScheduler,
        photosDirectory = "${documentsDirectory()}/photos",
        deviceId = getOrCreateDeviceId(),
    )
}

private fun getOrCreateDeviceId(): String {
    val defaults = NSUserDefaults.standardUserDefaults
    return defaults.stringForKey(DEVICE_ID_KEY) ?: NSUUID().UUIDString().also {
        defaults.setObject(it, forKey = DEVICE_ID_KEY)
    }
}

private fun documentsDirectory(): String =
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
