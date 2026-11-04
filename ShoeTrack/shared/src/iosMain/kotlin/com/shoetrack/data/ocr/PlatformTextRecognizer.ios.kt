package com.shoetrack.data.ocr

import com.shoetrack.domain.service.TextRecognizer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual fun createTextRecognizer(): TextRecognizer = VisionTextRecognizer()

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class VisionTextRecognizer : TextRecognizer {

    // Vision runs the request on the calling thread, so keep it off the main thread.
    override suspend fun recognize(image: ByteArray): String = withContext(Dispatchers.Default) {
        recognizeBlocking(image)
    }

    private suspend fun recognizeBlocking(image: ByteArray): String = suspendCancellableCoroutine { continuation ->
        val request = VNRecognizeTextRequest { finished, error ->
            if (error != null) {
                continuation.resumeWithException(RecognitionException(error))
                return@VNRecognizeTextRequest
            }
            val lines = finished?.results.orEmpty()
                .filterIsInstance<VNRecognizedTextObservation>()
                .mapNotNull { (it.topCandidates(1uL).firstOrNull() as? VNRecognizedText)?.string }
            continuation.resume(lines.joinToString("\n"))
        }.apply {
            recognitionLevel = VNRequestTextRecognitionLevelAccurate
            usesLanguageCorrection = true
            recognitionLanguages = listOf("es-ES", "en-US")
        }
        val handler = VNImageRequestHandler(data = image.toNSData(), options = emptyMap<Any?, Any?>())
        handler.performRequests(listOf(request), error = null)
    }
}

private class RecognitionException(error: NSError) : Exception(error.localizedDescription)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}
