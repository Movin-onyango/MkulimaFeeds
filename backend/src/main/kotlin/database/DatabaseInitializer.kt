package com.movofeeds.database

import com.movofeeds.database.tables.AuthAttemptsTable
import com.movofeeds.database.tables.NotificationsTable
import com.movofeeds.database.tables.OrderItemsTable
import com.movofeeds.database.tables.OrdersTable
import com.movofeeds.database.tables.ProductsTable
import com.movofeeds.database.tables.RoleAuditLogTable
import com.movofeeds.database.tables.SavedLocationsTable
import com.movofeeds.database.tables.SettingsTable
import com.movofeeds.database.tables.UsersTable
import com.movofeeds.database.tables.VerificationCodesTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseInitializer {

    fun initialize() {
        transaction {
            SchemaUtils.create(
                UsersTable,
                ProductsTable,
                OrdersTable,
                OrderItemsTable,
                NotificationsTable,
                VerificationCodesTable,
                AuthAttemptsTable,
                SavedLocationsTable,
                SettingsTable,
                RoleAuditLogTable
            )
        }
    }
}