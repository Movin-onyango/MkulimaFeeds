package com.mkulimafeeds.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkulimafeeds.data.repository.notification.NotificationRepository
import com.mkulimafeeds.domain.model.notification.RemoteNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<RemoteNotification> = emptyList(),
    val error: String? = null
) {
    val unreadCount: Int
        get() = notifications.count {
            !it.isRead
        }
}

class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            NotificationUiState()
        )

    val uiState: StateFlow<NotificationUiState> =
        _uiState.asStateFlow()

    fun loadNotifications(
        token: String
    ) {
        if (token.isBlank()) {
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    error = "Authentication token not found"
                )
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            try {

                val notifications =
                    repository.getNotifications(
                        token
                    )

                _uiState.value =
                    NotificationUiState(
                        isLoading = false,
                        notifications = notifications
                            .sortedByDescending {
                                it.createdAt
                            }
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error =
                            e.message
                                ?: "Failed to load notifications"
                    )
            }
        }
    }

    fun markAsRead(
        id: Long,
        token: String
    ) {
        viewModelScope.launch {

            try {

                val updated =
                    repository.markAsRead(
                        id = id,
                        token = token
                    )

                val updatedList =
                    _uiState.value.notifications
                        .map { notification ->

                            if (
                                notification.id == updated.id
                            ) {
                                updated
                            } else {
                                notification
                            }
                        }

                _uiState.value =
                    _uiState.value.copy(
                        notifications = updatedList,
                        error = null
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        error =
                            e.message
                                ?: "Failed to mark notification as read"
                    )
            }
        }
    }

    fun markAllAsRead(
        token: String
    ) {
        viewModelScope.launch {

            try {

                repository.markAllAsRead(
                    token
                )

                val updatedList =
                    _uiState.value.notifications
                        .map { notification ->
                            notification.copy(
                                isRead = true
                            )
                        }

                _uiState.value =
                    _uiState.value.copy(
                        notifications = updatedList,
                        error = null
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        error =
                            e.message
                                ?: "Failed to mark notifications as read"
                    )
            }
        }
    }

    fun clearError() {
        _uiState.value =
            _uiState.value.copy(
                error = null
            )
    }
}