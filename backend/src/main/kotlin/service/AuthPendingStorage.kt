package com.movofeeds.service

import java.util.concurrent.ConcurrentHashMap

/**
 * Holds partially-registered user data in memory until the
 * OTP is verified. Once verified, the account is created in
 * the users table and the pending entry is removed.
 *
 * Entries auto-expire after TTL_MS. A background sweep runs
 * lazily on every access — no scheduled executor needed for
 * a dev-scale deployment.
 *
 * PRODUCTION NOTE:
 *   In-memory storage does not survive a backend restart and
 *   will not work behind a load balancer with multiple
 *   instances. For production, replace this with Redis
 *   (preferred) or a database table.
 */
object AuthPendingStorage {

    private const val TTL_MS = 15L * 60L * 1000L // 15 minutes

    data class PendingRegistration(
        val name: String,
        val email: String?,
        val phone: String?,
        val passwordHash: String,
        val channel: String,     // "EMAIL" or "PHONE"
        val destination: String, // the email or phone the OTP was sent to
        val createdAt: Long
    )

    private val registrations =
        ConcurrentHashMap<String, PendingRegistration>()

    /**
     * Key is derived from the destination (email or phone).
     * One pending registration per destination at a time.
     */
    private fun key(destination: String): String =
        destination.trim().lowercase()

    fun save(reg: PendingRegistration) {
        sweep()
        registrations[key(reg.destination)] = reg
    }

    fun get(destination: String): PendingRegistration? {
        sweep()
        return registrations[key(destination)]
    }

    fun remove(destination: String) {
        registrations.remove(key(destination))
    }

    /**
     * Remove all expired entries. Called on every access — cheap.
     */
    private fun sweep() {
        val cutoff = System.currentTimeMillis() - TTL_MS
        val iterator = registrations.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value.createdAt < cutoff) {
                iterator.remove()
            }
        }
    }
}