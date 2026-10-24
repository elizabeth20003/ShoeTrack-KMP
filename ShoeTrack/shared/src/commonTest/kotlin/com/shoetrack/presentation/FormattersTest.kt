package com.shoetrack.presentation

import com.shoetrack.domain.model.Money
import com.shoetrack.presentation.format.format
import com.shoetrack.presentation.format.toMoneyOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormattersTest {

    @Test
    fun formatsMoneyWithThousandsSeparators() {
        assertEquals("$ 12.345", Money.fromUnits(12_345).format())
        assertEquals("$ 900", Money.fromUnits(900).format())
        assertEquals("$ 1.000.000", Money.fromUnits(1_000_000).format())
    }

    @Test
    fun parsesTypedTextAsWholeUnits() {
        assertEquals(Money.fromUnits(120_000), "120.000".toMoneyOrNull())
    }

    @Test
    fun textWithoutDigitsIsNotMoney() {
        assertNull("abc".toMoneyOrNull())
        assertNull("".toMoneyOrNull())
    }
}
