package com.movofeeds.security

import io.ktor.server.auth.jwt.JWTPrincipal

/**
 * Central place to enforce permissions.
 *
 * Usage:
 *
 *   RbacGuard.require(principal, Permission.MANAGE_PRODUCTS)
 *
 * Throws IllegalAccessException if the principal is
 * unauthenticated or lacks the required permission.
 * Callers catch this and return HTTP 403.
 */
object RbacGuard {

    /**
     * Throws if the principal does not have the permission.
     */
    fun require(
        principal: JWTPrincipal?,
        permission: Permission
    ) {
        val role = roleOf(principal)
            ?: throw IllegalAccessException(
                "Authentication required"
            )

        if (!RolePermissions.hasPermission(role, permission)) {
            throw IllegalAccessException(
                "Your role ($role) does not permit this action"
            )
        }
    }

    /**
     * Returns true if the principal has the permission.
     * Use this for optional checks (e.g., conditional UI logic).
     */
    fun check(
        principal: JWTPrincipal?,
        permission: Permission
    ): Boolean {
        val role = roleOf(principal) ?: return false
        return RolePermissions.hasPermission(role, permission)
    }

    /**
     * Extracts the role claim from the JWT principal.
     */
    private fun roleOf(principal: JWTPrincipal?): String? {
        return principal
            ?.payload
            ?.getClaim("role")
            ?.asString()
            ?.trim()
            ?.uppercase()
    }
}