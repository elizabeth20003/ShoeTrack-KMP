package com.shoetrack

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.shoetrack.domain.usecase.LoadPhotoUseCase
import com.shoetrack.presentation.navigation.AppNavigation
import com.shoetrack.presentation.photo.LocalLoadPhoto
import org.koin.mp.KoinPlatform

@Composable
fun App() {
    val loadPhoto = remember { KoinPlatform.getKoin().get<LoadPhotoUseCase>() }
    CompositionLocalProvider(LocalLoadPhoto provides loadPhoto) {
        MaterialTheme {
            AppNavigation()
        }
    }
}
