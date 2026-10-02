package com.movofeeds.repository

import com.movofeeds.database.tables.SavedLocationsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
data class SavedLocationRecord(
    val id: Long,
    val userId: Long,
    val label: String,
    val address: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

class SavedLocationRepository {

    fun findByUser(userId: Long): List<SavedLocationRecord> =
        transaction {
            SavedLocationsTable
                .selectAll()
                .where { SavedLocationsTable.userId eq userId }
                .orderBy(
                    SavedLocationsTable.isDefault to SortOrder.DESC,
                    SavedLocationsTable.createdAt to SortOrder.DESC
                )
                .map { it.toRecord() }
        }

    fun findById(id: Long): SavedLocationRecord? =
        transaction {
            SavedLocationsTable
                .selectAll()
                .where { SavedLocationsTable.id eq id }
                .singleOrNull()
                ?.toRecord()
        }

    fun create(
        userId: Long,
        label: String,
        address: String,
        isDefault: Boolean
    ): SavedLocationRecord =
        transaction {
            val now = System.currentTimeMillis()

            // If this is being set as default, unset all others first
            if (isDefault) {
                SavedLocationsTable.update(
                    where = { SavedLocationsTable.userId eq userId }
                ) {
                    it[SavedLocationsTable.isDefault] = false
                }
            }

            val id = SavedLocationsTable.insertAndGetId {
                it[SavedLocationsTable.userId] = userId
                it[SavedLocationsTable.label] = label
                it[SavedLocationsTable.address] = address
                it[SavedLocationsTable.isDefault] = isDefault
                it[SavedLocationsTable.createdAt] = now
                it[SavedLocationsTable.updatedAt] = now
            }.value

            findById(id) ?: error("Failed to create saved location")
        }

    fun update(
        id: Long,
        userId: Long,
        label: String,
        address: String,
        isDefault: Boolean
    ): SavedLocationRecord? =
        transaction {
            // Verify ownership
            val existing = SavedLocationsTable
                .selectAll()
                .where {
                    (SavedLocationsTable.id eq id) and
                            (SavedLocationsTable.userId eq userId)
                }
                .singleOrNull()
                ?: return@transaction null

            val now = System.currentTimeMillis()

            // If setting this as default, unset all others first
            if (isDefault) {
                SavedLocationsTable.update(
                    where = {
                        (SavedLocationsTable.userId eq userId) and
                                (SavedLocationsTable.id neq id)
                    }
                ) {
                    it[SavedLocationsTable.isDefault] = false
                }
            }

            SavedLocationsTable.update(
                where = { SavedLocationsTable.id eq id }
            ) {
                it[SavedLocationsTable.label] = label
                it[SavedLocationsTable.address] = address
                it[SavedLocationsTable.isDefault] = isDefault
                it[SavedLocationsTable.updatedAt] = now
            }

            findById(id)
        }

    fun delete(id: Long, userId: Long): Boolean =
        transaction {
            val deleted = SavedLocationsTable.deleteWhere {
                (SavedLocationsTable.id eq id) and
                        (SavedLocationsTable.userId eq userId)
            }
            deleted > 0
        }

    private fun ResultRow.toRecord(): SavedLocationRecord =
        SavedLocationRecord(
            id = this[SavedLocationsTable.id].value,
            userId = this[SavedLocationsTable.userId].value,
            label = this[SavedLocationsTable.label],
            address = this[SavedLocationsTable.address],
            isDefault = this[SavedLocationsTable.isDefault],
            createdAt = this[SavedLocationsTable.createdAt],
            updatedAt = this[SavedLocationsTable.updatedAt]
        )
}