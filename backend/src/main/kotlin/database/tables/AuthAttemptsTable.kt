package com.movofeeds.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

/**
 * Generic attempt log for rate-limiting.
 *
 * Rather than storing counter columns on user records
 * (which requires careful row-level locking), we log
 * each attempt and count rows within a time window.
 * Simple, correct, and horizontally scalable.
 *
 * Supported attempt types:
 *   OTP_REQUEST    → a request to send a verification code
 *   OTP_VERIFY     → a verification attempt (successful or not)
 *   LOGIN_FAIL     → a failed login attempt
 *   REGISTER_FAIL  → a failed registration attempt
 *
 * The identifier is typically:
 *   - an email address
 *   - a phone number
 *   - an IP address
 *   - a composite like "email:foo@bar.com"
 */
object AuthAttemptsTable : LongIdTable("auth_attempts") {

    val identifier = varchar("identifier", 255)
        .index()

    val attemptType = varchar("attempt_type", 30)
        .index()

    val createdAt = long("created_at")
        .index()
}