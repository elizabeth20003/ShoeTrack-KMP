package com.shoetrack.presentation.photo

import androidx.compose.runtime.Composable

/** Lets the user bring a photo into the app, from the camera or from existing images. */
interface PhotoPicker {
    fun takePhoto()

    fun pickFromGallery()
}

/** Longest side, in pixels, of the photos kept for shoes. */
const val STORED_PHOTO_SIDE_PX = 1280

/** Handwriting needs more detail than a shoe photo to be read. */
const val SCAN_PHOTO_SIDE_PX = 2560

/**
 * Remembers a [PhotoPicker] that reports each chosen image, as JPEG bytes, to [onPhotoPicked].
 * Platforms that scale photos keep their longest side at or under [maxSidePx].
 */
@Composable
expect fun rememberPhotoPicker(onPhotoPicked: (ByteArray) -> Unit, maxSidePx: Int = STORED_PHOTO_SIDE_PX): PhotoPicker
