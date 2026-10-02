package com.mkulimafeeds.data.remote.dto.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponseDto(
    val id: Long,
    val userId: Long,
    val orderId: Long? = null,
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