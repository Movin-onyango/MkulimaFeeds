package com.movofeeds.service

import com.movofeeds.security.OtpGenerator
import com.movofeeds.security.PasswordHasher
import com.movofeeds.security.VerificationCodeRecord
import com.movofeeds.security.VerificationCodeRepository
import org.slf4j.LoggerFactory
import kotlinx.coroutines.runBlocking


/**
 * Orchestrates the OTP lifecycle:
 *
 *   1. Check rate limit for the destination.
 *   2. Generate a fresh 6-digit code.
 *   3. Hash and store it (invalidating previous codes).
 *   4. Send via email or SMS (mocked in dev).
 *
 * And the verification side:
 *
 *   1. Look up the active code for the destination.
 *   2. Check it hasn't expired or exceeded attempts.
 *   3. Verify the bcrypt hash in constant time.
 *   4. Mark as verified on success, or increment attempts.
 */
class VerificationService(
    private val repository: VerificationCodeRepository =
        VerificationCodeRepository()
) {

    private val logger =
        LoggerFactory.getLogger(VerificationService::class.java)

    companion object {

        const val PURPOSE_REGISTER = "REGISTER"
        const val PURPOSE_PASSWORD_RESET = "PASSWORD_RESET"

        const val CHANNEL_EMAIL = "EMAIL"
        const val CHANNEL_PHONE = "PHONE"

        /** 10 minutes */
        const val CODE_TTL_MS = 10L * 60L * 1000L

        /** 3 requests per destination per hour */
        const val MAX_REQUESTS_PER_HOUR = 3
        const val ONE_HOUR_MS = 60L * 60L * 1000L

        /** 5 verification attempts per code */
        const val MAX_VERIFY_ATTEMPTS = 5
    }

    // =========================================================
    // SEND A NEW CODE
    // =========================================================

    /**
     * Creates, stores, and sends a fresh verification code.
     *
     * @return Result.success(Unit) if the code was sent,
     *         Result.failure(exception) if rate-limited or
     *         delivery failed.
     */
    suspend fun sendVerificationCode(
        userId: Long?,
        purpose: String,
        channel: String,
        destination: String
    ): Result<Unit> {
        try {
            // ---------------------------------------------
            // 1. Rate limit
            // ---------------------------------------------
            if (isRateLimited(destination, channel)) {
                return Result.failure(
                    IllegalStateException(
                        "Too many requests. Please wait before requesting another code."
                    )
                )
            }

            // ---------------------------------------------
            // 2. Invalidate previous active codes
            // ---------------------------------------------
            repository.invalidatePreviousCodes(
                destination = destination,
                purpose = purpose,
                channel = channel
            )

            // ---------------------------------------------
            // 3. Generate + hash
            // ---------------------------------------------
            val code = OtpGenerator.generateNumeric()
            val codeHash = PasswordHasher.hash(code)
            val expiresAt = System.currentTimeMillis() + CODE_TTL_MS

            // ---------------------------------------------
            // 4. Store
            // ---------------------------------------------
            repository.createCode(
                userId = userId,
                purpose = purpose,
                channel = channel,
                destination = destination,
                codeHash = codeHash,
                expiresAt = expiresAt
            )

            // ---------------------------------------------
            // 5. Record attempt for rate limiting
            // ---------------------------------------------
            repository.recordAttempt(
                identifier = rateLimitKey(destination, channel),
                attemptType = "OTP_REQUEST"
            )

            // ---------------------------------------------
            // 6. Send (mocked in dev)
            // ---------------------------------------------
            val sent = runBlocking {
                when (channel) {
                    CHANNEL_EMAIL -> MockSenderService.sendEmail(
                        toAddress = destination,
                        subject = subjectFor(purpose),
                        code = code
                    )

                    CHANNEL_PHONE -> MockSenderService.sendSms(
                        toPhone = destination,
                        message = messageFor(purpose),
                        code = code
                    )

                    else -> false
                }
            }
            return if (sent) {
                Result.success(Unit)
            } else {
                Result.failure(
                    IllegalStateException(
                        "Failed to send verification code. Please try again."
                    )
                )
            }

        } catch (e: Exception) {
            logger.error("Failed to send verification code", e)
            return Result.failure(e)
        }
    }

    // =========================================================
    // VERIFY A CODE
    // =========================================================

    /**
     * Verifies the code the user entered.
     *
     * @return Result.success(record) on success,
     *         Result.failure(exception) on mismatch, expiry,
     *         or too many attempts.
     */
    fun verifyCode(
        destination: String,
        purpose: String,
        channel: String,
        code: String
    ): Result<VerificationCodeRecord> {
        try {
            val record = repository.findActiveCode(
                destination = destination,
                purpose = purpose,
                channel = channel
            ) ?: return Result.failure(
                IllegalStateException(
                    "No active code found. Please request a new one."
                )
            )

            // ---------------------------------------------
            // Expiry re-check (belt + suspenders)
            // ---------------------------------------------
            if (record.expiresAt < System.currentTimeMillis()) {
                return Result.failure(
                    IllegalStateException(
                        "This code has expired. Please request a new one."
                    )
                )
            }

            // ---------------------------------------------
            // Attempt limit
            // ---------------------------------------------
            if (record.attempts >= MAX_VERIFY_ATTEMPTS) {
                return Result.failure(
                    IllegalStateException(
                        "Too many incorrect attempts. Please request a new code."
                    )
                )
            }

            // ---------------------------------------------
            // Hash comparison
            // ---------------------------------------------
            val matches = PasswordHasher.verify(
                password = code,
                passwordHash = record.codeHash
            )

            if (!matches) {
                repository.incrementAttempts(record.id)
                val remaining =
                    (MAX_VERIFY_ATTEMPTS - record.attempts - 1)
                        .coerceAtLeast(0)
                return Result.failure(
                    IllegalStateException(
                        "Incorrect code. $remaining attempts remaining."
                    )
                )
            }

            // ---------------------------------------------
            // Success
            // ---------------------------------------------
            repository.markVerified(
                id = record.id,
                timestamp = System.currentTimeMillis()
            )

            return Result.success(record)

        } catch (e: Exception) {
            logger.error("Failed to verify code", e)
            return Result.failure(e)
        }
    }

    // =========================================================
    // RATE LIMITING
    // =========================================================

    fun isRateLimited(
        destination: String,
        channel: String
    ): Boolean {
        val since = System.currentTimeMillis() - ONE_HOUR_MS
        val attempts = repository.countAttemptsSince(
            identifier = rateLimitKey(destination, channel),
            attemptType = "OTP_REQUEST",
            sinceMs = since
        )
        return attempts >= MAX_REQUESTS_PER_HOUR
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun rateLimitKey(
        destination: String,
        channel: String
    ): String = "${channel.lowercase()}:$destination"

    private fun subjectFor(purpose: String): String =
        when (purpose) {
            PURPOSE_REGISTER -> "Verify your MkulimaFeeds account"
            PURPOSE_PASSWORD_RESET -> "Reset your MkulimaFeeds password"
            else -> "Your MkulimaFeeds verification code"
        }

    private fun messageFor(purpose: String): String =
        when (purpose) {
            PURPOSE_REGISTER -> "Your MkulimaFeeds verification code is"
            PURPOSE_PASSWORD_RESET -> "Your MkulimaFeeds password reset code is"
            else -> "Your MkulimaFeeds code is"
        }
}