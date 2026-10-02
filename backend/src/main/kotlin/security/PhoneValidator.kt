package com.movofeeds.security

/**
 * Validates Kenyan phone numbers.
 *
 * Accepted formats:
 *   07XXXXXXXX    (Safaricom, Airtel, Telkom)
 *   01XXXXXXXX    (newer Safaricom, Airtel)
 *   +2547XXXXXXXX
 *   +2541XXXXXXXX
 *   2547XXXXXXXX
 *   2541XXXXXXXX
 *
 * All formats are normalized to `+2547XXXXXXXX` or
 * `+2541XXXXXXXX` before storage.
 *
 * Rejects:
 *   - Numbers shorter or longer than 10 digits (local)
 *   - Numbers with letters or symbols
 *   - Numbers with an invalid Kenyan prefix
 */
object PhoneValidator {

    private val KENYA_LOCAL_REGEX = Regex("^0[17]\\d{8}$")
    private val KENYA_INTL_REGEX = Regex("^\\+254[17]\\d{8}$")
    private val KENYA_RAW_REGEX = Regex("^254[17]\\d{8}$")

    /**
     * Returns true if the input is a valid Kenyan phone number
     * in any of the supported formats.
     */
    fun isValid(phone: String): Boolean {
        val cleaned = sanitize(phone)
        return KENYA_LOCAL_REGEX.matches(cleaned) ||
                KENYA_INTL_REGEX.matches(cleaned) ||
                KENYA_RAW_REGEX.matches(cleaned)
    }

    /**
     * Returns a human-readable rejection reason or null if valid.
     */
    fun rejectionReason(phone: String): String? {
        val cleaned = sanitize(phone)
        if (cleaned.isEmpty()) {
            return "Please enter a phone number"
        }
        if (!cleaned.all { it.isDigit() || it == '+' }) {
            return "Phone number must contain only digits"
        }
        if (!isValid(cleaned)) {
            return "Please enter a valid Kenyan phone number (e.g., 0712345678)"
        }
        return null
    }

    /**
     * Normalizes a phone number to the international format:
     *   +2547XXXXXXXX or +2541XXXXXXXX
     */
    fun normalize(phone: String): String {
        val cleaned = sanitize(phone)
        return when {
            KENYA_LOCAL_REGEX.matches(cleaned) ->
                "+254" + cleaned.substring(1)

            KENYA_RAW_REGEX.matches(cleaned) ->
                "+" + cleaned

            KENYA_INTL_REGEX.matches(cleaned) ->
                cleaned

            else -> cleaned
        }
    }

    /**
     * Strips spaces, dashes, and parentheses.
     * Keeps digits and a single leading '+'.
     */
    fun sanitize(phone: String): String {
        val trimmed = phone.trim()
        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        return if (hasPlus) "+$digitsOnly" else digitsOnly
    }
}