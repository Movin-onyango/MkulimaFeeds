package com.mkulimafeeds.domain.model.notification

data class RemoteNotification(
    val id: Long,
    val userId: Long,
    val orderId: Long?,
    val type: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: Long
)