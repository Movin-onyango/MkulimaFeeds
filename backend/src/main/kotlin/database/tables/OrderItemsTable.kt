package com.movofeeds.database.tables


import org.jetbrains.exposed.dao.id.LongIdTable

object OrderItemsTable : LongIdTable("order_items") {

    val orderId = reference(
        "order_id",
        OrdersTable
    )

    val productId = reference(
        "product_id",
        ProductsTable
    )

    val quantity = decimal(
        "quantity",
        precision = 12,
        scale = 2
    )

    val unitPrice = decimal(
        "unit_price",
        precision = 12,
        scale = 2
    )

    val subtotal = decimal(
        "subtotal",
        precision = 12,
        scale = 2
    )

    val createdAt = long("created_at")
}