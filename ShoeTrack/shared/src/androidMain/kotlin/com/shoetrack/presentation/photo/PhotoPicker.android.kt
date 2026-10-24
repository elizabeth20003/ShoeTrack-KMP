package com.shoetrack.presentation.photo

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

private const val CAPTURES_DIRECTORY = "captures"

@Composable
actual fun rememberPhotoPicker(onPhotoPicked: (ByteArray) -> Unit): PhotoPicker {
    val context = LocalContext.current
    val currentOnPhotoPicked by rememberUpdatedState(onPhotoPicked)
    // The camera app runs in another task, so the target file must survive this activity being recreated.
    var pendingCapturePath by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val capture = pendingCapturePath?.let(::File)
        if (taken && capture != null) {
            currentOnPhotoPicked(scaleToJpeg(capture.readBytes()))
        }
        capture?.delete()
        pendingCapturePath = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val bytes = uri?.let { context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() } }
        if (bytes != null) currentOnPhotoPicked(scaleToJpeg(bytes))
    }

    return remember(context) {
        object : PhotoPicker {
            override fun takePhoto() {
                val capture = createCaptureFile(context)
                pendingCapturePath = capture.path
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", capture)
                cameraLauncher.launch(uri)
            }

            override fun pickFromGallery() {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }
}

private fun createCaptureFile(context: Context): File {
    val directory = File(context.cacheDir, CAPTURES_DIRECTORY).apply { mkdirs() }
    return File.createTempFile("capture", ".jpg", directory)
}
