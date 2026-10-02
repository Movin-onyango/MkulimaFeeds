package com.movofeeds.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val id: Long,
    val userId: Long,
    val orderId: Long?,
    val type: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: Long
)

@Serializable
data class MarkNotificationReadRequest(
    val isRead: Boolean = true
)