package com.shoetrack.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneNumberRulesTest {

    @Test
    fun emptyPhoneIsValidBecauseItIsOptional() {
        assertTrue(PhoneNumberRules.isValid(""))
    }

    @Test
    fun tenDigitMobileStartingWithThreeIsValid() {
        assertTrue(PhoneNumberRules.isValid("3001234567"))
        assertTrue(PhoneNumberRules.isValid("300 123 4567"))
    }

    @Test
    fun wrongLengthOrPrefixIsInvalid() {
        assertFalse(PhoneNumberRules.isValid("300123456"))
        assertFalse(PhoneNumberRules.isValid("30012345678"))
        assertFalse(PhoneNumberRules.isValid("2001234567"))
    }

    @Test
    fun normalizeKeepsOnlyDigits() {
        assertEquals("3001234567", PhoneNumberRules.normalize("(300) 123-4567"))
    }
}
