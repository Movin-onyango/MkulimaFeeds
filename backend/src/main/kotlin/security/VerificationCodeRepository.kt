package com.movofeeds.security

import com.movofeeds.database.tables.AuthAttemptsTable
import com.movofeeds.database.tables.VerificationCodesTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * Data-access layer for verification codes and rate-limit attempts.
 *
 * This class knows nothing about HTTP, emails, or SMS — it only
 * persists and retrieves data. The VerificationService orchestrates
 * the business logic on top.
 */
class VerificationCodeRepository {

    // =========================================================
    // VERIFICATION CODES
    // =========================================================

    fun createCode(
        userId: Long?,
        purpose: String,
        channel: String,
        destination: String,
        codeHash: String,
        expiresAt: Long
    ): Long =
        transaction {
            VerificationCodesTable.insert {
                it[VerificationCodesTable.userId] = userId
                it[VerificationCodesTable.purpose] = purpose
                it[VerificationCodesTable.channel] = channel
                it[VerificationCodesTable.destination] = destination
                it[VerificationCodesTable.codeHash] = codeHash
                it[VerificationCodesTable.expiresAt] = expiresAt
                it[VerificationCodesTable.attempts] = 0
                it[VerificationCodesTable.createdAt] = System.currentTimeMillis()
            } get VerificationCodesTable.id
        }.value

    /**
     * Finds the most recent active (unverified, unexpired) code
     * for a given destination and purpose.
     */
    fun findActiveCode(
        destination: String,
        purpose: String,
        channel: String
    ): VerificationCodeRecord? =
        transaction {
            val now = System.currentTimeMillis()
            VerificationCodesTable
                .selectAll()
                .where {
                    (VerificationCodesTable.destination eq destination) and
                            (VerificationCodesTable.purpose eq purpose) and
                            (VerificationCodesTable.channel eq channel) and
                            (VerificationCodesTable.verifiedAt.isNull()) and
                            (VerificationCodesTable.expiresAt greater now)
                }
                .orderBy(
                    VerificationCodesTable.createdAt,
                    SortOrder.DESC
                )
                .limit(1)
                .singleOrNull()
                ?.toRecord()
        }

    fun findById(id: Long): VerificationCodeRecord? =
        transaction {
            VerificationCodesTable
                .selectAll()
                .where { VerificationCodesTable.id eq id }
                .singleOrNull()
                ?.toRecord()
        }

    fun incrementAttempts(id: Long) {
        transaction {
            val current = VerificationCodesTable
                .selectAll()
                .where { VerificationCodesTable.id eq id }
                .singleOrNull()
                ?: return@transaction

            val currentAttempts = current[VerificationCodesTable.attempts]
            VerificationCodesTable.update(
                where = { VerificationCodesTable.id eq id }
            ) {
                it[attempts] = currentAttempts + 1
            }
        }
    }

    fun markVerified(id: Long, timestamp: Long) {
        transaction {
            VerificationCodesTable.update(
                where = { VerificationCodesTable.id eq id }
            ) {
                it[verifiedAt] = timestamp
            }
        }
    }

    /**
     * Invalidate all previous codes for the same
     * destination + purpose + channel.
     * Called when a new code is generated, so only the
     * latest code is ever valid.
     */
    fun invalidatePreviousCodes(
        destination: String,
        purpose: String,
        channel: String
    ) {
        val now = System.currentTimeMillis()
        transaction {
            VerificationCodesTable.update(
                where = {
                    (VerificationCodesTable.destination eq destination) and
                            (VerificationCodesTable.purpose eq purpose) and
                            (VerificationCodesTable.channel eq channel) and
                            (VerificationCodesTable.verifiedAt.isNull())
                }
            ) {
                it[verifiedAt] = now
            }
        }
    }

    // =========================================================
    // AUTH ATTEMPTS (rate limiting)
    // =========================================================

    fun recordAttempt(
        identifier: String,
        attemptType: String
    ) {
        transaction {
            AuthAttemptsTable.insert {
                it[AuthAttemptsTable.identifier] = identifier
                it[AuthAttemptsTable.attemptType] = attemptType
                it[AuthAttemptsTable.createdAt] = System.currentTimeMillis()
            }
        }
    }

    /**
     * Counts attempts of a given type for a given identifier
     * since a given timestamp. Used to enforce rate limits.
     */
    fun countAttemptsSince(
        identifier: String,
        attemptType: String,
        sinceMs: Long
    ): Int =
        transaction {
            AuthAttemptsTable
                .selectAll()
                .where {
                    (AuthAttemptsTable.identifier eq identifier) and
                            (AuthAttemptsTable.attemptType eq attemptType) and
                            (AuthAttemptsTable.createdAt greaterEq sinceMs)
                }
                .count()
                .toInt()
        }

    // =========================================================
    // MAPPING
    // =========================================================

    private fun org.jetbrains.exposed.sql.ResultRow.toRecord():
            VerificationCodeRecord =
        VerificationCodeRecord(
            id = this[VerificationCodesTable.id].value,
            userId = this[VerificationCodesTable.userId],
            purpose = this[VerificationCodesTable.purpose],
            channel = this[VerificationCodesTable.channel],
            destination = this[VerificationCodesTable.destination],
            codeHash = this[VerificationCodesTable.codeHash],
            expiresAt = this[VerificationCodesTable.expiresAt],
            verifiedAt = this[VerificationCodesTable.verifiedAt],
            attempts = this[VerificationCodesTable.attempts],
            createdAt = this[VerificationCodesTable.createdAt]
        )
}

data class VerificationCodeRecord(
    val id: Long,
    val userId: Long?,
    val purpose: String,
    val channel: String,
    val destination: String,
    val codeHash: String,
    val expiresAt: Long,
    val verifiedAt: Long?,
    val attempts: Int,
    val createdAt: Long
)