package com.mkulimafeeds

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object NotificationManager {
    private val notifications = mutableListOf<Notification>()

    fun addNotification(title: String, message: String, type: NotificationType, recipientId: String) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timestamp = sdf.format(Date())
        
        notifications.add(0, Notification(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            timestamp = timestamp,
            type = type,
            recipientId = recipientId
        ))
    }

    fun getNotificationsForUser(recipientId: String): List<Notification> {
        return notifications.filter { it.recipientId == recipientId }
    }

    fun getAllNotifications(): List<Notification> = notifications

    fun markAsRead(notificationId: String) {
        notifications.find { it.id == notificationId }?.isRead = true
    }

    fun clearAll() {
        notifications.clear()
    }
}
