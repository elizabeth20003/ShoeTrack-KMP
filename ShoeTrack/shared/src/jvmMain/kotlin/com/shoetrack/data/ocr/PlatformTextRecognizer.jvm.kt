package com.shoetrack.data.ocr

import com.shoetrack.domain.service.TextRecognizer

actual fun createTextRecognizer(): TextRecognizer = UnsupportedTextRecognizer

/** The desktop build has no text recognition; scanning is a phone feature. */
private object UnsupportedTextRecognizer : TextRecognizer {
    override suspend fun recognize(image: ByteArray): String =
        throw UnsupportedOperationException("Text recognition is not available on desktop")
}
