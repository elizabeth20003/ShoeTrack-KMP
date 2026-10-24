package com.shoetrack.presentation.photo

import androidx.compose.runtime.Composable

/** Lets the user bring a photo into the app, from the camera or from existing images. */
interface PhotoPicker {
    fun takePhoto()
    fun pickFromGallery()
}

/** Remembers a [PhotoPicker] that reports each chosen image, as JPEG bytes, to [onPhotoPicked]. */
@Composable
expect fun rememberPhotoPicker(onPhotoPicked: (ByteArray) -> Unit): PhotoPicker
