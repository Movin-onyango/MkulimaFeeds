package com.movofeeds.security

/**
 * Validates email addresses.
 *
 * Uses two layers:
 *   1. A regex check for basic RFC-compliant syntax.
 *   2. A blocklist of known disposable email providers.
 *
 * We deliberately do NOT use a strict whitelist of "real"
 * providers (gmail, yahoo, etc.) because that would lock out
 * legitimate business users with custom domains
 * (e.g., "ceo@company.co.ke").
 *
 * The strongest validation is the OTP itself: a user with a
 * fake email can never complete registration because the code
 * is never delivered.
 */
object EmailValidator {

    /**
     * Simple but practical email regex.
     * Rejects obvious garbage without being overly strict.
     */
    private val EMAIL_REGEX =
        Regex(
            pattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            option = RegexOption.IGNORE_CASE
        )

    /**
     * Common disposable / temporary email providers.
     * Requests to register with these domains are rejected.
     * Add more as needed — this list is not exhaustive but
     * covers the most abused services.
     */
    private val DISPOSABLE_DOMAINS = setOf(
        "mailinator.com",
        "tempmail.com",
        "temp-mail.org",
        "guerrillamail.com",
        "10minutemail.com",
        "throwawaymail.com",
        "yopmail.com",
        "sharklasers.com",
        "trashmail.com",
        "getnada.com",
        "dispostable.com",
        "mintemail.com",
        "maildrop.cc",
        "fakeinbox.com",
        "spamgourmet.com",
        "mytemp.email",
        "moakt.com",
        "emailondeck.com",
        "tempr.email",
        "mailcatch.com"
    )

    /**
     * Returns true if the email is syntactically valid
     * AND not from a known disposable provider.
     */
    fun isValid(email: String): Boolean {
        val trimmed = email.trim().lowercase()
        if (!EMAIL_REGEX.matches(trimmed)) {
            return false
        }
        val domain = trimmed.substringAfterLast("@")
        return domain !in DISPOSABLE_DOMAINS
    }

    /**
     * Returns a human-readable reason the email was rejected.
     * Null if valid.
     */
    fun rejectionReason(email: String): String? {
        val trimmed = email.trim().lowercase()
        if (!EMAIL_REGEX.matches(trimmed)) {
            return "Please enter a valid email address"
        }
        val domain = trimmed.substringAfterLast("@")
        if (domain in DISPOSABLE_DOMAINS) {
            return "Disposable email addresses are not allowed. Please use a permanent email."
        }
        return null
    }

    /**
     * Normalizes an email for storage.
     * Lowercase + trim. Callers should use this before saving.
     */
    fun normalize(email: String): String =
        email.trim().lowercase()
}