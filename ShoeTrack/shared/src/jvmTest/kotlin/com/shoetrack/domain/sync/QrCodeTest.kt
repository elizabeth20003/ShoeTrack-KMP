package com.shoetrack.domain.sync

import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QrCodeTest {

    // A real decoder is the only honest check that the code can be scanned.
    private fun decode(qr: QrCode): String {
        val scale = 6
        val margin = 4
        val side = (qr.size + 2 * margin) * scale
        val pixels = ByteArray(side * side) { index ->
            val x = index % side / scale - margin
            val y = index / side / scale - margin
            val dark = x in 0 until qr.size && y in 0 until qr.size && qr.isDark(x, y)
            if (dark) 0 else 0xFF.toByte()
        }
        val source = object : LuminanceSource(side, side) {
            override fun getRow(y: Int, row: ByteArray?): ByteArray = pixels.copyOfRange(y * side, (y + 1) * side)

            override fun getMatrix(): ByteArray = pixels

            override fun isCropSupported() = false
        }
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
    }

    @Test
    fun aPairingCodeCanBeScanned() {
        val text = "shoetrack:1:192.168.100.123:45872"

        assertEquals(text, decode(QrCode.encode(text)))
    }

    @Test
    fun shortAndLongTextsCanBeScanned() {
        listOf("a", "shoetrack:1:10.0.0.5:1025", "x".repeat(40), "y".repeat(100), "z".repeat(106)).forEach { text ->
            assertEquals(text, decode(QrCode.encode(text)))
        }
    }

    @Test
    fun refusesTextThatDoesNotFit() {
        assertFailsWith<IllegalArgumentException> { QrCode.encode("x".repeat(107)) }
    }
}
