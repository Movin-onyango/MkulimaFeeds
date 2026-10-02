package com.movofeeds.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

/**
 * Configurable business rules.
 *
 * Every value is stored as a string with an explicit
 * type tag, so we can parse it correctly at read time
 * and validate on write.
 *
 * Supported value types:
 *   INT      → Integer
 *   DECIMAL  → BigDecimal
 *   STRING   → plain String
 *   BOOLEAN  → "true" / "false"
 *   ENUM     → one of a predefined set (validation done in service)
 *
 * Categories group settings for admin UI:
 *   BULK_ORDER  → bulk order rules
 *   DEALER      → dealer promotion / approval rules
 *   GENERAL     → catch-all
 *
 * Keys use dot notation: "bulk_order.min_value"
 * This is a design convention, not enforced by the DB.
 */
object SettingsTable : LongIdTable("settings") {

    /**
     * Unique key for this setting.
     * Convention: category.subcategory.name
     * Example: "bulk_order.min_value"
     */
    val key = varchar("key", 100)
        .uniqueIndex()

    /**
     * The actual value, always stored as a string.
     * Parsed based on valueType when read.
     */
    val value = text("value")

    /**
     * INT | DECIMAL | STRING | BOOLEAN | ENUM
     */
    val valueType = varchar("value_type", 20)

    /**
     * BULK_ORDER | DEALER | GENERAL
     * Used for grouping in the admin UI.
     */
    val category = varchar("category", 50)
        .index()

    /**
     * Human-readable description shown in the admin form.
     */
    val description = varchar("description", 500)

    /**
     * Last modification timestamp (epoch ms).
     */
    val updatedAt = long("updated_at")

    /**
     * Who last modified this setting (for audit).
     * Nullable because seed values have no author.
     */
    val updatedByUserId = long("updated_by_user_id")
        .nullable()
}