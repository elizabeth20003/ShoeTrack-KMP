package com.shoetrack.domain.sync

/**
 * A QR code for short texts: byte mode, error correction level M, versions 1 to 6.
 * The layout and the Reed-Solomon maths follow ISO/IEC 18004.
 */
class QrCode private constructor(val size: Int, private val modules: Array<BooleanArray>) {
    /** True when the module at ([x], [y]) is dark. */
    fun isDark(x: Int, y: Int): Boolean = modules[y][x]

    companion object {
        private const val MAX_VERSION = 6
        private const val FORMAT_BITS_LEVEL_M = 0
        private val ECC_CODEWORDS_PER_BLOCK = intArrayOf(10, 16, 26, 18, 24, 16)
        private val BLOCK_COUNT = intArrayOf(1, 1, 1, 2, 2, 4)
        private val PAD_BYTES = intArrayOf(0xEC, 0x11)

        /** Throws [IllegalArgumentException] when [text] does not fit in a version 6 code. */
        fun encode(text: String): QrCode {
            val bytes = text.encodeToByteArray()
            val version =
                (1..MAX_VERSION).firstOrNull { bytes.size <= dataCodewords(it) - 2 }
                    ?: throw IllegalArgumentException("The text is too long for a QR code")
            val codewords = interleaveWithErrorCorrection(version, dataBytes(version, bytes))
            return Builder(version).build(codewords)
        }

        private fun rawModules(version: Int): Int {
            var result = (16 * version + 128) * version + 64
            if (version >= 2) {
                val alignments = version / 7 + 2
                result -= (25 * alignments - 10) * alignments - 55
            }
            return result
        }

        private fun dataCodewords(version: Int): Int =
            rawModules(version) / 8 - ECC_CODEWORDS_PER_BLOCK[version - 1] * BLOCK_COUNT[version - 1]

        private fun dataBytes(version: Int, payload: ByteArray): ByteArray {
            val bits = BitBuffer()
            bits.append(0b0100, 4)
            bits.append(payload.size, 8)
            payload.forEach { bits.append(it.toInt() and 0xFF, 8) }
            val capacityBits = dataCodewords(version) * 8
            bits.append(0, minOf(4, capacityBits - bits.length))
            bits.append(0, (8 - bits.length % 8) % 8)
            var pad = 0
            while (bits.length < capacityBits) {
                bits.append(PAD_BYTES[pad % 2], 8)
                pad++
            }
            return bits.toBytes()
        }

        private fun interleaveWithErrorCorrection(version: Int, data: ByteArray): ByteArray {
            val blocks = BLOCK_COUNT[version - 1]
            val eccLength = ECC_CODEWORDS_PER_BLOCK[version - 1]
            val rawCodewords = rawModules(version) / 8
            val shortBlocks = blocks - rawCodewords % blocks
            val shortBlockLength = rawCodewords / blocks
            val divisor = reedSolomonDivisor(eccLength)

            var offset = 0
            val parts =
                (0 until blocks).map { index ->
                    val dataLength = shortBlockLength - eccLength + if (index < shortBlocks) 0 else 1
                    val block = data.copyOfRange(offset, offset + dataLength)
                    offset += dataLength
                    val ecc = reedSolomonRemainder(block, divisor)
                    // Short blocks get a placeholder so every block has the same length when interleaving.
                    val padded = if (index < shortBlocks) block + 0.toByte() else block
                    padded + ecc
                }

            val result = ArrayList<Byte>(rawCodewords)
            for (i in parts[0].indices) {
                for ((j, block) in parts.withIndex()) {
                    if (i != shortBlockLength - eccLength || j >= shortBlocks) result.add(block[i])
                }
            }
            return result.toByteArray()
        }

        private fun reedSolomonDivisor(degree: Int): ByteArray {
            val result = ByteArray(degree)
            result[degree - 1] = 1
            var root = 1
            repeat(degree) {
                for (j in result.indices) {
                    result[j] = multiply(result[j].toInt() and 0xFF, root).toByte()
                    if (j + 1 < result.size) result[j] = (result[j].toInt() xor result[j + 1].toInt()).toByte()
                }
                root = multiply(root, 0x02)
            }
            return result
        }

        private fun reedSolomonRemainder(data: ByteArray, divisor: ByteArray): ByteArray {
            val result = ByteArray(divisor.size)
            for (b in data) {
                val factor = (b.toInt() xor result[0].toInt()) and 0xFF
                result.copyInto(result, 0, 1, result.size)
                result[result.size - 1] = 0
                for (i in result.indices) {
                    result[i] = (result[i].toInt() xor multiply(divisor[i].toInt() and 0xFF, factor)).toByte()
                }
            }
            return result
        }

        // Multiplication in GF(2^8) with the QR polynomial x^8 + x^4 + x^3 + x^2 + 1.
        private fun multiply(x: Int, y: Int): Int {
            var z = 0
            for (i in 7 downTo 0) {
                z = (z shl 1) xor ((z ushr 7) * 0x11D)
                z = z xor (((y ushr i) and 1) * x)
            }
            return z
        }
    }

    private class BitBuffer {
        private val bits = ArrayList<Boolean>()
        val length get() = bits.size

        fun append(value: Int, count: Int) {
            for (i in count - 1 downTo 0) bits.add(((value ushr i) and 1) != 0)
        }

        fun toBytes(): ByteArray {
            val bytes = ByteArray(bits.size / 8)
            bits.forEachIndexed { i, bit -> if (bit) bytes[i / 8] = (bytes[i / 8].toInt() or (1 shl (7 - i % 8))).toByte() }
            return bytes
        }
    }

    private class Builder(private val version: Int) {
        private val size = version * 4 + 17
        private val modules = Array(size) { BooleanArray(size) }
        private val isFunction = Array(size) { BooleanArray(size) }

        fun build(codewords: ByteArray): QrCode {
            drawFunctionPatterns()
            drawCodewords(codewords)
            var bestMask = 0
            var bestPenalty = Int.MAX_VALUE
            for (mask in 0..7) {
                applyMask(mask)
                drawFormatBits(mask)
                val penalty = penalty()
                if (penalty < bestPenalty) {
                    bestPenalty = penalty
                    bestMask = mask
                }
                applyMask(mask)
            }
            applyMask(bestMask)
            drawFormatBits(bestMask)
            return QrCode(size, modules)
        }

        private fun setFunction(x: Int, y: Int, dark: Boolean) {
            modules[y][x] = dark
            isFunction[y][x] = true
        }

        private fun drawFunctionPatterns() {
            for (i in 0 until size) {
                setFunction(6, i, i % 2 == 0)
                setFunction(i, 6, i % 2 == 0)
            }
            drawFinder(3, 3)
            drawFinder(size - 4, 3)
            drawFinder(3, size - 4)
            val positions = alignmentPositions()
            for (i in positions.indices) {
                for (j in positions.indices) {
                    val onFinder = (i == 0 && j == 0) || (i == 0 && j == positions.lastIndex) || (i == positions.lastIndex && j == 0)
                    if (!onFinder) drawAlignment(positions[i], positions[j])
                }
            }
            drawFormatBits(0)
        }

        private fun alignmentPositions(): IntArray {
            if (version == 1) return IntArray(0)
            val count = version / 7 + 2
            val step = (version * 4 + count * 2 + 1) / (count * 2 - 2) * 2
            val result = IntArray(count)
            result[0] = 6
            var position = size - 7
            for (i in count - 1 downTo 1) {
                result[i] = position
                position -= step
            }
            return result
        }

        private fun drawFinder(centerX: Int, centerY: Int) {
            for (dy in -4..4) {
                for (dx in -4..4) {
                    val distance = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                    val x = centerX + dx
                    val y = centerY + dy
                    if (x in 0 until size && y in 0 until size) setFunction(x, y, distance != 2 && distance != 4)
                }
            }
        }

        private fun drawAlignment(centerX: Int, centerY: Int) {
            for (dy in -2..2) {
                for (dx in -2..2) {
                    setFunction(centerX + dx, centerY + dy, maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != 1)
                }
            }
        }

        private fun drawFormatBits(mask: Int) {
            val data = (FORMAT_BITS_LEVEL_M shl 3) or mask
            var remainder = data
            repeat(10) { remainder = (remainder shl 1) xor ((remainder ushr 9) * 0x537) }
            val bits = ((data shl 10) or remainder) xor 0x5412

            fun bit(i: Int) = ((bits ushr i) and 1) != 0

            for (i in 0..5) setFunction(8, i, bit(i))
            setFunction(8, 7, bit(6))
            setFunction(8, 8, bit(7))
            setFunction(7, 8, bit(8))
            for (i in 9..14) setFunction(14 - i, 8, bit(i))

            for (i in 0..7) setFunction(size - 1 - i, 8, bit(i))
            for (i in 8..14) setFunction(8, size - 15 + i, bit(i))
            setFunction(8, size - 8, true)
        }

        private fun drawCodewords(codewords: ByteArray) {
            var bitIndex = 0
            var right = size - 1
            while (right >= 1) {
                if (right == 6) right = 5
                for (vertical in 0 until size) {
                    for (j in 0..1) {
                        val x = right - j
                        val upward = ((right + 1) and 2) == 0
                        val y = if (upward) size - 1 - vertical else vertical
                        if (!isFunction[y][x] && bitIndex < codewords.size * 8) {
                            modules[y][x] = ((codewords[bitIndex ushr 3].toInt() ushr (7 - (bitIndex and 7))) and 1) != 0
                            bitIndex++
                        }
                    }
                }
                right -= 2
            }
        }

        private fun applyMask(mask: Int) {
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val invert =
                        when (mask) {
                            0 -> (x + y) % 2 == 0
                            1 -> y % 2 == 0
                            2 -> x % 3 == 0
                            3 -> (x + y) % 3 == 0
                            4 -> (x / 3 + y / 2) % 2 == 0
                            5 -> x * y % 2 + x * y % 3 == 0
                            6 -> (x * y % 2 + x * y % 3) % 2 == 0
                            else -> ((x + y) % 2 + x * y % 3) % 2 == 0
                        }
                    if (invert && !isFunction[y][x]) modules[y][x] = !modules[y][x]
                }
            }
        }

        // A simplified version of the standard penalty: long runs, 2x2 blocks and dark/light balance.
        private fun penalty(): Int {
            var result = 0
            for (horizontal in listOf(true, false)) {
                for (a in 0 until size) {
                    var run = 1
                    for (b in 1 until size) {
                        val same = if (horizontal) modules[a][b] == modules[a][b - 1] else modules[b][a] == modules[b - 1][a]
                        if (same) {
                            run++
                            if (run == 5) {
                                result += 3
                            } else if (run > 5) {
                                result++
                            }
                        } else {
                            run = 1
                        }
                    }
                }
            }
            for (y in 0 until size - 1) {
                for (x in 0 until size - 1) {
                    val color = modules[y][x]
                    if (color == modules[y][x + 1] && color == modules[y + 1][x] && color == modules[y + 1][x + 1]) result += 3
                }
            }
            val dark = modules.sumOf { row -> row.count { it } }
            val total = size * size
            result += kotlin.math.abs(dark * 20 - total * 10) / total * 10
            return result
        }
    }
}
