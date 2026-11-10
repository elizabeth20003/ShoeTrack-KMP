package com.shoetrack.presentation.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max

private const val JPEG_QUALITY = 85

/** Downscales a photo and bakes in its EXIF rotation, so stored images are small and upright. */
internal fun scaleToJpeg(source: ByteArray, maxSidePx: Int): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(source, 0, source.size, bounds)

    val options =
        BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(max(bounds.outWidth, bounds.outHeight), maxSidePx)
        }
    val decoded = BitmapFactory.decodeByteArray(source, 0, source.size, options) ?: return source

    val upright = decoded.rotatedBy(exifRotationDegrees(source))
    val output = ByteArrayOutputStream()
    upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
    return output.toByteArray()
}

private fun sampleSizeFor(longestSide: Int, maxSidePx: Int): Int {
    var sample = 1
    while (longestSide / (sample * 2) >= maxSidePx) sample *= 2
    return sample
}

private fun exifRotationDegrees(source: ByteArray): Float = when (
    ExifInterface(ByteArrayInputStream(source))
        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
    else -> 0f
}

private fun Bitmap.rotatedBy(degrees: Float): Bitmap {
    if (degrees == 0f) return this
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
