package com.movofeeds.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object UsersTable : LongIdTable("users") {

    val name = varchar("name", 150)

    /**
     * Nullable because users can register with email-only.
     * When null, the DB stores NULL (not an empty string),
     * so the unique index doesn't collide across multiple
     * email-only users.
     */
    val phone = varchar("phone", 20)
        .nullable()
        .uniqueIndex()

    /**
     * Nullable because users can register with phone-only.
     */
    val email = varchar("email", 150)
        .nullable()
        .uniqueIndex()

    val passwordHash = varchar("password_hash", 255)

    /**
     * CUSTOMER | DEALER | STAFF | ADMIN
     */
    val role = varchar("role", 30)

    /**
     * ACTIVE | PENDING | SUSPENDED | INACTIVE
     *
     * Default ACTIVE for all self-registered users.
     * PENDING is used when a user applies for a role that
     * requires admin approval (e.g., dealer application).
     * SUSPENDED blocks login entirely.
     * INACTIVE is the soft-delete state.
     */
    val status = varchar("status", 20)
        .default("ACTIVE")
        .index()

    /**
     * null for non-dealers.
     * PENDING | APPROVED | SUSPENDED | REVOKED for dealer accounts.
     *
     * Kept separate from `role` because a user can have
     * role = DEALER but dealer_status = PENDING during an
     * approval window, or dealer_status = SUSPENDED when the
     * dealership has been paused without changing the role.
     */
    val dealerStatus = varchar("dealer_status", 20)
        .nullable()
        .index()

    val isActive = bool("is_active")
        .default(true)

    val createdAt = long("created_at")

    val updatedAt = long("updated_at")

    // Dealer-specific fields (nullable for non-dealers)
    val businessName = varchar("business_name", 200).nullable()
    val businessRegion = varchar("business_region", 100).nullable()
    val accountManagerName = varchar("account_manager_name", 150).nullable()
    val accountManagerEmail = varchar("account_manager_email", 200).nullable()
    val creditLimit = decimal("credit_limit", 14, 2).default(java.math.BigDecimal.ZERO)
    val creditUsed = decimal("credit_used", 14, 2).default(java.math.BigDecimal.ZERO)
    val paymentTerms = varchar("payment_terms", 20).default("PREPAID")
    val lifetimeBulkOrders = integer("lifetime_bulk_orders").default(0)
}