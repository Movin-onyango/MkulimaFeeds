package com.movofeeds.repository

import com.movofeeds.database.tables.RoleAuditLogTable
import com.movofeeds.database.tables.UsersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class RoleAuditRecord(
    val id: Long,
    val userId: Long,
    val oldRole: String,
    val newRole: String,
    val changedByUserId: Long,
    val reason: String?,
    val createdAt: Long
)

class RoleAuditLogRepository {

    fun log(
        userId: Long,
        oldRole: String,
        newRole: String,
        changedByUserId: Long,
        reason: String?
    ): RoleAuditRecord =
        transaction {
            val id = RoleAuditLogTable.insertAndGetId {
                it[RoleAuditLogTable.userId] = userId
                it[RoleAuditLogTable.oldRole] = oldRole
                it[RoleAuditLogTable.newRole] = newRole
                it[RoleAuditLogTable.changedByUserId] = changedByUserId
                it[RoleAuditLogTable.reason] = reason
                it[RoleAuditLogTable.createdAt] = System.currentTimeMillis()
            }.value

            findById(id) ?: error("Failed to log role change")
        }

    fun findById(id: Long): RoleAuditRecord? =
        transaction {
            RoleAuditLogTable
                .selectAll()
                .where { RoleAuditLogTable.id eq id }
                .singleOrNull()
                ?.toRecord()
        }

    /**
     * Recent role changes, joined with the user's name and
     * the admin's name for display.
     */
    fun findRecent(limit: Int = 100): List<RoleAuditRecord> =
        transaction {
            RoleAuditLogTable
                .selectAll()
                .orderBy(
                    RoleAuditLogTable.createdAt to SortOrder.DESC
                )
                .limit(limit)
                .map { it.toRecord() }
        }

    fun findByUserId(userId: Long): List<RoleAuditRecord> =
        transaction {
            RoleAuditLogTable
                .selectAll()
                .where { RoleAuditLogTable.userId eq userId }
                .orderBy(
                    RoleAuditLogTable.createdAt to SortOrder.DESC
                )
                .map { it.toRecord() }
        }

    private fun ResultRow.toRecord(): RoleAuditRecord =
        RoleAuditRecord(
            id = this[RoleAuditLogTable.id].value,
            userId = this[RoleAuditLogTable.userId].value,
            oldRole = this[RoleAuditLogTable.oldRole],
            newRole = this[RoleAuditLogTable.newRole],
            changedByUserId = this[RoleAuditLogTable.changedByUserId].value,
            reason = this[RoleAuditLogTable.reason],
            createdAt = this[RoleAuditLogTable.createdAt]
        )
}