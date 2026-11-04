package com.shoetrack.domain.service

/** Reads the text in a photo, such as a page of the paper notebook. */
interface TextRecognizer {
    /** Returns the text found in [image] (JPEG bytes), one line per line of text. */
    suspend fun recognize(image: ByteArray): String
}
