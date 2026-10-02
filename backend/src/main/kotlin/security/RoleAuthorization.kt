package com.movofeeds.security

import io.ktor.server.auth.jwt.JWTPrincipal

/**
 * Legacy helper. Prefer RbacGuard for new code.
 *
 * Kept for backward compatibility with existing call sites
 * during the migration to the RbacGuard engine.
 */
object RoleAuthorization {

    const val ADMIN = "ADMIN"
    const val STAFF = "STAFF"
    const val CUSTOMER = "CUSTOMER"
    const val DEALER = "DEALER"

    /**
     * Returns true if the principal has management-level
     * access. Delegates to the central permission engine.
     */
    fun hasManagementRole(
        principal: JWTPrincipal
    ): Boolean {
        return RbacGuard.check(
            principal,
            Permission.MANAGE_PRODUCTS
        )
    }
}