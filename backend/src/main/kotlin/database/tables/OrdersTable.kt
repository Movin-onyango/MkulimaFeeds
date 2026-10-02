package com.movofeeds.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object OrdersTable : LongIdTable("orders") {

    val customerId = reference(
        "customer_id",
        UsersTable
    )

    val assignedDealerId = reference(
        "assigned_dealer_id",
        UsersTable
    ).nullable()

    val telephone = varchar("telephone", 20)

    val location = varchar("location", 255)

    val neededDate = long("needed_date")

    val status = varchar("status", 30)
        .default("PENDING")

    val notes = text("notes")
        .nullable()

    val orderType = varchar("order_type", 20)
        .default("RETAIL")
        .index()
    val totalAmount = decimal(
        "total_amount",
        precision = 12,
        scale = 2
    ).default(java.math.BigDecimal.ZERO)

    val createdAt = long("created_at")

    val updatedAt = long("updated_at")
}