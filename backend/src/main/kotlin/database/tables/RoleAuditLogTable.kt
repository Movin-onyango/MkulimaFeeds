package com.movofeeds.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

/**
 * Append-only audit trail for role changes.
 *
 * Every promotion, demotion, suspension, and reactivation
 * is recorded here with:
 *   - who was changed
 *   - what the old role was
 *   - what the new role is
 *   - who made the change
 *   - an optional reason
 *
 * Never updated or deleted. Rows accumulate as history.
 */
object RoleAuditLogTable : LongIdTable("role_audit_log") {

    /**
     * The user whose role changed.
     */
    val userId = reference(
        "user_id",
        UsersTable
    ).index()

    val oldRole = varchar("old_role", 30)

    val newRole = varchar("new_role", 30)

    /**
     * The admin who made the change.
     */
    val changedByUserId = reference(
        "changed_by_user_id",
        UsersTable
    )

    /**
     * Optional reason provided by the admin.
     * Example: "Approved dealer application for Nyeri region"
     */
    val reason = varchar("reason", 500)
        .nullable()

    val createdAt = long("created_at")
        .index()
}