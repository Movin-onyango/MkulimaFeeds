package com.movofeeds.security

/**
 * The single source of truth: which role has which permissions.
 *
 * To change who can do what, edit this file only.
 * The rest of the backend reads from here via RbacGuard.
 *
 * Note: A role's permission set is explicit, not hierarchical.
 * If you want DEALER to inherit CUSTOMER's permissions, both
 * sets must include the shared permissions. This makes each
 * role's capabilities self-documenting.
 */
object RolePermissions {

    private val roleMap: Map<String, Set<Permission>> = mapOf(

        // =====================================================
        // CUSTOMER — retail buyer
        // =====================================================
        "CUSTOMER" to setOf(
            Permission.BROWSE_CATALOG,
            Permission.PLACE_RETAIL_ORDER,
            Permission.VIEW_OWN_ORDERS,
            Permission.CANCEL_OWN_ORDER,
            Permission.VIEW_OWN_PROFILE,
            Permission.EDIT_OWN_PROFILE,
            Permission.MANAGE_OWN_LOCATIONS,
            Permission.DELETE_OWN_ACCOUNT,
        ),

        // =====================================================
        // DEALER — bulk buyer + fulfiller
        //
        // A dealer is both:
        //   1. A wholesale buyer from the main store
        //   2. A fulfiller of orders assigned to them
        //
        // So dealers inherit all customer permissions plus
        // their own.
        // =====================================================
        "DEALER" to setOf(
            // Customer-level
            Permission.BROWSE_CATALOG,
            Permission.PLACE_RETAIL_ORDER,
            Permission.VIEW_OWN_ORDERS,
            Permission.CANCEL_OWN_ORDER,
            Permission.VIEW_OWN_PROFILE,
            Permission.EDIT_OWN_PROFILE,
            Permission.MANAGE_OWN_LOCATIONS,
            Permission.DELETE_OWN_ACCOUNT,
            // Dealer-specific
            Permission.PLACE_BULK_ORDER,
            Permission.VIEW_DEALER_PRICING,
            Permission.VIEW_ASSIGNED_ORDERS,
            Permission.UPDATE_ASSIGNED_ORDER_STATUS,
            Permission.VIEW_ASSIGNED_CUSTOMERS,
        ),

        // =====================================================
        // STAFF — internal operations
        //
        // Can manage products, orders, and view analytics.
        // Cannot manage users, roles, or settings.
        // =====================================================
        "STAFF" to setOf(
            // Customer-level
            Permission.BROWSE_CATALOG,
            Permission.PLACE_RETAIL_ORDER,
            Permission.VIEW_OWN_ORDERS,
            Permission.CANCEL_OWN_ORDER,
            Permission.VIEW_OWN_PROFILE,
            Permission.EDIT_OWN_PROFILE,
            Permission.MANAGE_OWN_LOCATIONS,
            Permission.DELETE_OWN_ACCOUNT,
            // Staff-specific
            Permission.VIEW_ALL_ORDERS,
            Permission.UPDATE_ANY_ORDER_STATUS,
            Permission.ASSIGN_DEALERS_TO_ORDERS,
            Permission.MANAGE_PRODUCTS,
            Permission.MANAGE_UPLOADS,
            Permission.VIEW_ANALYTICS,
            Permission.VIEW_ALL_USERS,
        ),

        // =====================================================
        // ADMIN — full control
        //
        // Every permission in the system.
        // =====================================================
        "ADMIN" to Permission.values().toSet(),
    )

    /**
     * Get the permission set for a role.
     * Unknown role → empty set (deny by default).
     */
    fun permissionsFor(role: String): Set<Permission> =
        roleMap[role.trim().uppercase()] ?: emptySet()

    /**
     * Check if a role has a specific permission.
     */
    fun hasPermission(role: String, permission: Permission): Boolean =
        permission in permissionsFor(role)

    /**
     * All roles known to the system.
     */
    fun knownRoles(): Set<String> = roleMap.keys
}