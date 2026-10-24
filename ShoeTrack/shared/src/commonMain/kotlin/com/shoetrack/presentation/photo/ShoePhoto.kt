package com.shoetrack.presentation.photo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.shoetrack.domain.usecase.LoadPhotoUseCase
import com.shoetrack.presentation.common.runCatchingExceptCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Provided once at the app root so any screen can show a stored photo by name. */
val LocalLoadPhoto = compositionLocalOf<LoadPhotoUseCase> {
    error("LocalLoadPhoto was not provided")
}

@Composable
fun ShoePhoto(name: String, modifier: Modifier = Modifier) {
    val loadPhoto = LocalLoadPhoto.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = name) {
        value = withContext(Dispatchers.Default) {
            val bytes = loadPhoto(name) ?: return@withContext null
            runCatchingExceptCancellation { bytes.decodeToImageBitmap() }.getOrNull()
        }
    }

    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
