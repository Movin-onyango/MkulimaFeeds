package com.mkulimafeeds.data.repository.notification

import com.mkulimafeeds.data.mapper.notification.toDomain
import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.domain.model.notification.RemoteNotification

class NotificationRepository(
    private val apiService: ApiService
) {

    suspend fun getNotifications(
        token: String
    ): List<RemoteNotification> {
        return apiService
            .getNotifications(token)
            .map { it.toDomain() }
    }

    suspend fun markAsRead(
        id: Long,
        token: String
    ): RemoteNotification {
        return apiService
            .markNotificationAsRead(
                id = id,
                token = token
            )
            .toDomain()
    }

    suspend fun markAllAsRead(
        token: String
    ) {
        apiService.markAllNotificationsAsRead(
            token
        )
    }
}