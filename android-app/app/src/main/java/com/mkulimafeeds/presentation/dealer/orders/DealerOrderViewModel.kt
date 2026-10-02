package com.mkulimafeeds.presentation.dealer.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkulimafeeds.data.repository.dealer.DealerOrderRepository
import com.mkulimafeeds.domain.model.dealer.DealerOrder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DealerOrderUiState(
    val isLoading: Boolean = false,
    val orders: List<DealerOrder> = emptyList(),
    val selectedOrder: DealerOrder? = null,
    val error: String? = null,
    val statusUpdateLoading: Boolean = false
)

class DealerOrderViewModel(
    private val repository: DealerOrderRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            DealerOrderUiState()
        )

    val uiState: StateFlow<DealerOrderUiState> =
        _uiState.asStateFlow()

    private val _statusUpdateSuccess =
        MutableSharedFlow<DealerOrder>(
            extraBufferCapacity = 1
        )

    val statusUpdateSuccess: SharedFlow<DealerOrder> =
        _statusUpdateSuccess.asSharedFlow()

    fun loadAssignedOrders(
        token: String
    ) {

        if (token.isBlank()) {

            _uiState.value =
                _uiState.value.copy(
                    error =
                        "Authentication token not found"
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

                val orders =
                    repository.getAssignedOrders(
                        token = token
                    )

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        orders = orders,
                        error = null
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error =
                            e.message
                                ?: "Failed to load assigned orders"
                    )
            }
        }
    }

    fun loadAssignedOrder(
        id: Long,
        token: String
    ) {

        if (token.isBlank()) {

            _uiState.value =
                _uiState.value.copy(
                    error =
                        "Authentication token not found"
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

                val order =
                    repository.getAssignedOrder(
                        id = id,
                        token = token
                    )

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        selectedOrder = order,
                        error = null
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error =
                            e.message
                                ?: "Failed to load order"
                    )
            }
        }
    }

    fun updateOrderStatus(
        id: Long,
        status: String,
        token: String
    ) {

        if (token.isBlank()) {

            _uiState.value =
                _uiState.value.copy(
                    error =
                        "Authentication token not found"
                )

            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    statusUpdateLoading = true,
                    error = null
                )

            try {

                val updatedOrder =
                    repository.updateOrderStatus(
                        id = id,
                        status = status,
                        token = token
                    )

                val updatedOrders =
                    _uiState.value.orders.map { order ->

                        if (
                            order.id ==
                            updatedOrder.id
                        ) {
                            updatedOrder
                        } else {
                            order
                        }
                    }

                _uiState.value =
                    _uiState.value.copy(
                        statusUpdateLoading = false,
                        orders = updatedOrders,
                        selectedOrder =
                            updatedOrder,
                        error = null
                    )

                _statusUpdateSuccess.emit(
                    updatedOrder
                )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        statusUpdateLoading = false,
                        error =
                            e.message
                                ?: "Failed to update order status"
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