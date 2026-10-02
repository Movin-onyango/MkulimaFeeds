package com.movofeeds.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object NotificationsTable : LongIdTable("notifications") {

    val userId =
        long("user_id").index()

    val orderId =
        long("order_id")
            .nullable()
            .index()

    val type =
        varchar("type", 50)

    val title =
        varchar("title", 200)

    val message =
        varchar("message", 500)

    val isRead =
        bool("is_read")
            .default(false)

    val createdAt =
        long("created_at")
}