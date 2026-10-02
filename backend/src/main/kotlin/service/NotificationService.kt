package com.movofeeds.service

import com.movofeeds.repository.NotificationRecord
import com.movofeeds.repository.NotificationRepository

class NotificationService(
    private val notificationRepository:
    NotificationRepository =
        NotificationRepository()
) {

    fun createNotification(
        userId: Long,
        orderId: Long?,
        type: String,
        title: String,
        message: String
    ): NotificationRecord {

        require(userId > 0) {
            "Invalid notification user ID"
        }

        require(type.isNotBlank()) {
            "Notification type is required"
        }

        require(title.isNotBlank()) {
            "Notification title is required"
        }

        require(message.isNotBlank()) {
            "Notification message is required"
        }

        return notificationRepository.create(
            userId = userId,
            orderId = orderId,
            type = type.trim().uppercase(),
            title = title.trim(),
            message = message.trim(),
            createdAt =
                System.currentTimeMillis()
        )
            ?: throw IllegalStateException(
                "Failed to create notification"
            )
    }

    fun getUserNotifications(
        userId: Long
    ): List<NotificationRecord> {

        require(userId > 0) {
            "Invalid notification user ID"
        }

        return notificationRepository
            .findByUserId(userId)
    }

    fun markAsRead(
        userId: Long,
        notificationId: Long
    ): NotificationRecord {

        require(userId > 0) {
            "Invalid notification user ID"
        }

        require(notificationId > 0) {
            "Invalid notification ID"
        }

        return notificationRepository.markAsRead(
            id = notificationId,
            userId = userId
        )
            ?: throw NoSuchElementException(
                "Notification not found"
            )
    }

    fun markAllAsRead(
        userId: Long
    ) {

        require(userId > 0) {
            "Invalid notification user ID"
        }

        notificationRepository
            .markAllAsRead(userId)
    }
}