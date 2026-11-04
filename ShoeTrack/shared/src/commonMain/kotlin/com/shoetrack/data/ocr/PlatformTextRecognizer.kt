package com.shoetrack.data.ocr

import com.shoetrack.domain.service.TextRecognizer

/** The text recognizer of this platform: ML Kit on Android, Vision on iOS. */
expect fun createTextRecognizer(): TextRecognizer
