package com.shoetrack.domain.validation

/** Colombian mobile numbers: ten digits starting with 3. A phone number is optional. */
object PhoneNumberRules {

    private const val MOBILE_LENGTH = 10
    private const val MOBILE_PREFIX = '3'

    fun normalize(raw: String): String = raw.filter(Char::isDigit)

    fun isValid(raw: String): Boolean {
        val digits = normalize(raw)
        return digits.isEmpty() || (digits.length == MOBILE_LENGTH && digits.first() == MOBILE_PREFIX)
    }
}
