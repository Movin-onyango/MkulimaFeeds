package com.movofeeds.repository

import com.movofeeds.database.tables.SettingsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class SettingRecord(
    val id: Long,
    val key: String,
    val value: String,
    val valueType: String,
    val category: String,
    val description: String,
    val updatedAt: Long,
    val updatedByUserId: Long?
)

class SettingsRepository {

    fun findAll(): List<SettingRecord> =
        transaction {
            SettingsTable
                .selectAll()
                .orderBy(
                    SettingsTable.category to SortOrder.ASC,
                    SettingsTable.key to SortOrder.ASC
                )
                .map { it.toRecord() }
        }

    fun findByCategory(category: String): List<SettingRecord> =
        transaction {
            SettingsTable
                .selectAll()
                .where { SettingsTable.category eq category }
                .orderBy(SettingsTable.key to SortOrder.ASC)
                .map { it.toRecord() }
        }

    fun findByKey(key: String): SettingRecord? =
        transaction {
            SettingsTable
                .selectAll()
                .where { SettingsTable.key eq key }
                .singleOrNull()
                ?.toRecord()
        }

    /**
     * Update a single setting value.
     * Returns the updated record, or null if not found.
     */
    fun updateValue(
        key: String,
        newValue: String,
        adminId: Long
    ): SettingRecord? =
        transaction {
            val existing = SettingsTable
                .selectAll()
                .where { SettingsTable.key eq key }
                .singleOrNull()
                ?: return@transaction null

            SettingsTable.update(
                where = { SettingsTable.key eq key }
            ) {
                it[value] = newValue
                it[updatedAt] = System.currentTimeMillis()
                it[updatedByUserId] = adminId
            }

            findByKey(key)
        }

    /**
     * Bulk update — use a single transaction for all changes.
     */
    fun updateAll(
        changes: List<Pair<String, String>>,
        adminId: Long
    ): List<SettingRecord> =
        transaction {
            val now = System.currentTimeMillis()
            changes.forEach { (key, newValue) ->
                SettingsTable.update(
                    where = { SettingsTable.key eq key }
                ) {
                    it[value] = newValue
                    it[updatedAt] = now
                    it[updatedByUserId] = adminId
                }
            }
            changes.mapNotNull { (key, _) -> findByKey(key) }
        }

    private fun ResultRow.toRecord(): SettingRecord =
        SettingRecord(
            id = this[SettingsTable.id].value,
            key = this[SettingsTable.key],
            value = this[SettingsTable.value],
            valueType = this[SettingsTable.valueType],
            category = this[SettingsTable.category],
            description = this[SettingsTable.description],
            updatedAt = this[SettingsTable.updatedAt],
            updatedByUserId = this[SettingsTable.updatedByUserId]
        )
}