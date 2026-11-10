package com.shoetrack.presentation.pairing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
actual fun rememberQrScanner(onCode: (String) -> Unit): QrScanner? {
    val context = LocalContext.current
    val currentOnCode by rememberUpdatedState(onCode)
    return remember(context) {
        object : QrScanner {
            // Google's scanner screen brings its own camera UI, so the app needs no camera permission.
            override fun scan() {
                val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                GmsBarcodeScanning
                    .getClient(context, options)
                    .startScan()
                    .addOnSuccessListener { barcode -> barcode.rawValue?.let(currentOnCode) }
            }
        }
    }
}
