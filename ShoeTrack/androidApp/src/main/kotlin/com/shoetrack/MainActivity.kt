package com.shoetrack

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shoetrack.notification.NotificationIntents

class MainActivity : ComponentActivity() {

    private var layawayToOpen by mutableStateOf<String?>(null)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        layawayToOpen = layawayIdFrom(intent)
        askForNotificationPermission()

        setContent {
            App(
                layawayToOpen = layawayToOpen,
                onLayawayOpened = { layawayToOpen = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        layawayIdFrom(intent)?.let { layawayToOpen = it }
    }

    private fun layawayIdFrom(intent: Intent?): String? =
        intent?.getStringExtra(NotificationIntents.EXTRA_LAYAWAY_ID)

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
