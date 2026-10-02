package com.movofeeds.security

/**
 * Every discrete action a user can perform in the system.
 *
 * Adding a new permission = adding one enum value + one
 * entry in RolePermissions. Nothing else changes.
 *
 * Naming convention: VERB_NOUN
 */
enum class Permission {

    // -----------------------------------------------------
    // Customer-level (all authenticated users)
    // -----------------------------------------------------
    BROWSE_CATALOG,
    PLACE_RETAIL_ORDER,
    VIEW_OWN_ORDERS,
    CANCEL_OWN_ORDER,
    VIEW_OWN_PROFILE,
    EDIT_OWN_PROFILE,
    MANAGE_OWN_LOCATIONS,
    DELETE_OWN_ACCOUNT,

    // -----------------------------------------------------
    // Dealer-specific
    // -----------------------------------------------------
    PLACE_BULK_ORDER,
    VIEW_DEALER_PRICING,
    VIEW_ASSIGNED_ORDERS,
    UPDATE_ASSIGNED_ORDER_STATUS,
    VIEW_ASSIGNED_CUSTOMERS,

    // -----------------------------------------------------
    // Staff-level (management operations)
    // -----------------------------------------------------
    VIEW_ALL_ORDERS,
    UPDATE_ANY_ORDER_STATUS,
    ASSIGN_DEALERS_TO_ORDERS,
    MANAGE_PRODUCTS,
    MANAGE_UPLOADS,
    VIEW_ANALYTICS,
    VIEW_ALL_USERS,

    // -----------------------------------------------------
    // Admin-only
    // -----------------------------------------------------
    MANAGE_SETTINGS,
    PROMOTE_DEALERS,
    DEMOTE_DEALERS,
    CHANGE_USER_ROLES,
    SUSPEND_USERS,
    DEACTIVATE_USERS,
    CREATE_STAFF,
    VIEW_AUDIT_LOG,
}