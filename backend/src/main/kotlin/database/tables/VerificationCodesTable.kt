package com.movofeeds.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

/**
 * Stores OTP codes for verification flows.
 *
 * Supported purposes:
 *   REGISTER         → verify identity during signup
 *   PASSWORD_RESET   → verify identity before allowing password change
 *
 * Supported channels:
 *   EMAIL            → code sent to an email address
 *   PHONE            → code sent via SMS
 *
 * Security guarantees:
 *   - code_hash is bcrypt-hashed (never stored in plaintext)
 *   - expires_at is 10 minutes from creation
 *   - attempts is capped at 5 (see VerificationService)
 *   - verified_at being non-null marks single-use
 */
object VerificationCodesTable : LongIdTable("verification_codes") {

    /**
     * The user this code is for.
     * NULL during REGISTER before the user record exists.
     */
    val userId = long("user_id")
        .nullable()
        .index()

    /**
     * 'REGISTER' or 'PASSWORD_RESET'.
     */
    val purpose = varchar("purpose", 30)

    /**
     * 'EMAIL' or 'PHONE'.
     */
    val channel = varchar("channel", 10)

    /**
     * The email or phone the code was sent to.
     */
    val destination = varchar("destination", 255)
        .index()

    /**
     * bcrypt hash of the 6-digit code.
     */
    val codeHash = varchar("code_hash", 255)

    /**
     * Millisecond epoch timestamp when this code expires.
     */
    val expiresAt = long("expires_at")

    /**
     * Millisecond epoch timestamp when this code was verified.
     * NULL until verified. Non-null means the code is consumed.
     */
    val verifiedAt = long("verified_at")
        .nullable()

    /**
     * Failed verification attempt counter.
     * Once it reaches 5, the code is invalidated.
     */
    val attempts = integer("attempts")
        .default(0)

    val createdAt = long("created_at")
}