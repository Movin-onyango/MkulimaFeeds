package com.movofeeds.security

import java.security.SecureRandom

/**
 * Generates cryptographically secure numeric OTP codes.
 *
 * Uses java.security.SecureRandom (not kotlin.random.Random)
 * because the code must be unpredictable — an attacker must
 * not be able to guess the next code from the previous one.
 */
object OtpGenerator {

    private const val CODE_LENGTH = 6

    private val secureRandom = SecureRandom()

    /**
     * Generates a 6-digit numeric code as a String.
     * Padded with leading zeros so "42" becomes "000042".
     */
    fun generate(): String {
        val bound = 1_000_000 // 10^6
        val value = secureRandom.nextInt(bound)
        return value.toString().padStart(CODE_LENGTH, '0')
    }

    /**
     * Generates a 6-digit code for display purposes.
     * Same as generate() — kept separate in case we ever
     * want different formats (e.g., alphanumeric).
     */
    fun generateNumeric(): String = generate()
}