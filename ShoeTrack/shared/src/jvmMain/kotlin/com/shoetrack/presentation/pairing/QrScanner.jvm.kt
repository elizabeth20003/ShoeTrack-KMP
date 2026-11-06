package com.shoetrack.presentation.pairing

import androidx.compose.runtime.Composable

// The desktop has no camera to read a code with; its code can be pasted on the other device instead.
@Composable
actual fun rememberQrScanner(onCode: (String) -> Unit): QrScanner? = null
