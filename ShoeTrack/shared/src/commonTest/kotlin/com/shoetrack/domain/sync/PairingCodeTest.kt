package com.shoetrack.domain.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PairingCodeTest {

    @Test
    fun readsBackTheCodeItEncoded() {
        val code = PairingCode("192.168.1.23", 45872)

        assertEquals(code, PairingCode.parse(code.encode()))
    }

    @Test
    fun ignoresSpacesAroundTheCode() {
        assertEquals(PairingCode("10.0.0.5", 5000), PairingCode.parse("  shoetrack:1:10.0.0.5:5000\n"))
    }

    @Test
    fun rejectsTextThatIsNotACode() {
        assertNull(PairingCode.parse("hello"))
        assertNull(PairingCode.parse("https://example.com"))
        assertNull(PairingCode.parse("shoetrack:1:192.168.1:5000"))
        assertNull(PairingCode.parse("shoetrack:1:192.168.1.300:5000"))
        assertNull(PairingCode.parse("shoetrack:1:192.168.1.2:0"))
        assertNull(PairingCode.parse("shoetrack:1:192.168.1.2:70000"))
        assertNull(PairingCode.parse("shoetrack:1:192.168.1.2"))
    }
}
