package com.mkulimafeeds.data.mapper.notification

import com.mkulimafeeds.data.remote.dto.notification.NotificationResponseDto
import com.mkulimafeeds.domain.model.notification.RemoteNotification

fun NotificationResponseDto.toDomain(): RemoteNotification {
    return RemoteNotification(
        id = id,
        userId = userId,
        orderId = orderId,
        type = type,
        title = title,
        message = message,
        isRead = isRead,
        createdAt = createdAt
    )
}