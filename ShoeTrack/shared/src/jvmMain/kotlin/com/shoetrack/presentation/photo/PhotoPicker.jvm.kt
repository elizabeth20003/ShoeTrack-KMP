package com.shoetrack.presentation.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** Desktop has no camera, so both actions open a file chooser. */
@Composable
actual fun rememberPhotoPicker(onPhotoPicked: (ByteArray) -> Unit): PhotoPicker {
    val currentOnPhotoPicked = rememberUpdatedState(onPhotoPicked)
    return remember {
        object : PhotoPicker {
            override fun takePhoto() = pickFromGallery()

            override fun pickFromGallery() {
                val dialog = FileDialog(null as Frame?, "Select a photo", FileDialog.LOAD).apply { isVisible = true }
                val file = dialog.file?.let { File(dialog.directory, it) } ?: return
                currentOnPhotoPicked.value(file.readBytes())
            }
        }
    }
}
