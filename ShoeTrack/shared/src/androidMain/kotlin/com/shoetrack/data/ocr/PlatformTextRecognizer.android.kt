package com.shoetrack.data.ocr

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.shoetrack.domain.service.TextRecognizer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual fun createTextRecognizer(): TextRecognizer = MlKitTextRecognizer()

private class MlKitTextRecognizer : TextRecognizer {
    private val client by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun recognize(image: ByteArray): String {
        val bitmap =
            requireNotNull(BitmapFactory.decodeByteArray(image, 0, image.size)) {
                "The photo could not be decoded"
            }
        return suspendCancellableCoroutine { continuation ->
            client
                .process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { continuation.resume(it.text) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
    }
}
