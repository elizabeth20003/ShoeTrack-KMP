package com.shoetrack.presentation

import com.shoetrack.domain.model.LayawayState
import com.shoetrack.domain.model.LayawaySummary
import com.shoetrack.domain.model.Money
import com.shoetrack.presentation.list.matches
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class LayawaySearchTest {

    private val layaway = LayawaySummary(
        id = "1",
        customerName = "María Pérez",
        productDescription = "Nike Air Force 42",
        productPhotoPath = "",
        remainingBalance = Money(10_000),
        state = LayawayState.Active,
        lastPaymentAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    @Test
    fun blankQueryMatchesEverything() {
        assertTrue(layaway.matches(""))
        assertTrue(layaway.matches("   "))
    }

    @Test
    fun ignoresCaseAndAccents() {
        assertTrue(layaway.matches("maria perez"))
        assertTrue(layaway.matches("MARÍA"))
    }

    @Test
    fun matchesPartOfTheName() {
        assertTrue(layaway.matches("per"))
    }

    @Test
    fun matchesTheShoesToo() {
        assertTrue(layaway.matches("nike"))
    }

    @Test
    fun everyWordMustMatch() {
        assertTrue(layaway.matches("maria nike"))
        assertFalse(layaway.matches("maria adidas"))
    }
}
