package com.shoetrack

import android.app.Application
import com.shoetrack.data.local.AndroidDatabaseDriverFactory
import com.shoetrack.di.initKoin
import com.shoetrack.notification.AndroidOverdueReminderScheduler
import java.io.File

class ShoeTrackApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin(
            driverFactory = AndroidDatabaseDriverFactory(this),
            reminderScheduler = AndroidOverdueReminderScheduler(this),
            photosDirectory = File(filesDir, "photos").path,
            deviceId = DeviceId.getOrCreate(this),
        )
    }
}
