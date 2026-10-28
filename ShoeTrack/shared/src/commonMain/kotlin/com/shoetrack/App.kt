package com.shoetrack

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.shoetrack.domain.sync.SyncEngine
import com.shoetrack.domain.usecase.LoadPhotoUseCase
import com.shoetrack.presentation.navigation.AppNavigation
import com.shoetrack.presentation.photo.LocalLoadPhoto
import org.koin.mp.KoinPlatform

@Composable
fun App(
    layawayToOpen: String? = null,
    onLayawayOpened: () -> Unit = {},
) {
    val koin = remember { KoinPlatform.getKoin() }
    val loadPhoto = remember { koin.get<LoadPhotoUseCase>() }
    val syncEngine = remember { koin.get<SyncEngine>() }
    DisposableEffect(syncEngine) {
        syncEngine.start()
        onDispose { syncEngine.stop() }
    }
    CompositionLocalProvider(LocalLoadPhoto provides loadPhoto) {
        MaterialTheme {
            AppNavigation(layawayToOpen, onLayawayOpened)
        }
    }
}
