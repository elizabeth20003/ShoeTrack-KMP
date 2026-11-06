package com.shoetrack.presentation.pairing

import androidx.compose.runtime.Composable

/** Reads a QR code with the device camera. */
interface QrScanner {
    fun scan()
}

/**
 * Remembers a [QrScanner] that reports the text of the code it reads to [onCode], or null on
 * platforms without a camera to read it with.
 */
@Composable
expect fun rememberQrScanner(onCode: (String) -> Unit): QrScanner?
