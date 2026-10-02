package com.movofeeds.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object ProductsTable : LongIdTable("products") {

    val name = varchar("name", 150)

    val description = text("description")
        .nullable()

    val category = varchar("category", 100)

    val unit = varchar("unit", 30)

    val price = decimal(
        "price",
        precision = 12,
        scale = 2
    )

    val stockQuantity = decimal(
        "stock_quantity",
        precision = 12,
        scale = 2
    ).default(java.math.BigDecimal.ZERO)

    val isActive = bool("is_active")
        .default(true)

    val createdAt = long("created_at")

    val updatedAt = long("updated_at")
}