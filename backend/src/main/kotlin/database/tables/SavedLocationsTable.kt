package com.movofeeds.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

/**
 * Saved delivery locations for a customer.
 * A user can save multiple addresses (farm, home, office)
 * and pick one at checkout.
 */
object SavedLocationsTable : LongIdTable("saved_locations") {

    val userId = reference(
        "user_id",
        UsersTable
    ).index()

    val label = varchar("label", 50)

    val address = varchar("address", 500)

    val isDefault = bool("is_default")
        .default(false)

    val createdAt = long("created_at")

    val updatedAt = long("updated_at")
}