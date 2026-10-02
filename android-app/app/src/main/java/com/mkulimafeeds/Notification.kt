package com.mkulimafeeds

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val type: NotificationType,
    val recipientId: String, // Can be dealerId or farmerId
    var isRead: Boolean = false
)

enum class NotificationType {
    ORDER_ASSIGNED,
    ORDER_STATUS_UPDATE,
    STOCK_ALERT,
    PROMOTION,
    EXPERT_TIP
}
