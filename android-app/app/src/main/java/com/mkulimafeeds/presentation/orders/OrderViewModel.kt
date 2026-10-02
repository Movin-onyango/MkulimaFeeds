package com.mkulimafeeds.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkulimafeeds.Order
import com.mkulimafeeds.data.remote.CreateOrderRequest
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrderUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class OrderViewModel : ViewModel() {

    private val repository =
        OrderRepository(NetworkModule.apiService)

    private val _uiState =
        MutableStateFlow(OrderUiState())

    val uiState: StateFlow<OrderUiState> =
        _uiState.asStateFlow()

    // =========================================================
    // ADMIN
    // =========================================================

    fun loadAdminOrders(token: String) {
        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            try {

                val orders =
                    repository.getAdminOrders(token)

                _uiState.value =
                    _uiState.value.copy(
                        orders = orders,
                        isLoading = false,
                        error = null
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error =
                            e.message
                                ?: "Failed to load orders"
                    )
            }
        }
    }

    fun loadAdminOrder(
        id: Long,
        token: String,
        onSuccess: (Order) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {

            try {

                val order =
                    repository.getAdminOrder(
                        id = id,
                        token = token
                    )

                onSuccess(order)

            } catch (e: Exception) {

                onError(
                    e.message
                        ?: "Failed to load order"
                )
            }
        }
    }

    fun updateOrderStatus(
        id: Long,
        status: String,
        token: String,
        onSuccess: (Order) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {

            try {

                val updatedOrder =
                    repository.updateOrderStatus(
                        id = id,
                        status = status,
                        token = token
                    )

                onSuccess(updatedOrder)

            } catch (e: Exception) {

                onError(
                    e.message
                        ?: "Failed to update order status"
                )
            }
        }
    }

    fun assignDealer(
        id: Long,
        dealerId: Long?,
        token: String,
        onSuccess: (Order) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {

            try {

                val updatedOrder =
                    repository.assignDealer(
                        id = id,
                        dealerId = dealerId,
                        token = token
                    )

                onSuccess(updatedOrder)

            } catch (e: Exception) {

                onError(
                    e.message
                        ?: "Failed to assign dealer"
                )
            }
        }
    }

    // =========================================================
    // CUSTOMER
    // =========================================================

    fun loadMyOrders(token: String) {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            try {

                val orders =
                    repository.getMyOrders(token)

                _uiState.value =
                    _uiState.value.copy(
                        orders = orders,
                        isLoading = false,
                        error = null
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error =
                            e.message
                                ?: "Failed to load your orders"
                    )
            }
        }
    }

    fun loadMyOrder(
        id: Long,
        token: String,
        onSuccess: (Order) -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch {

            try {

                val order =
                    repository.getMyOrder(
                        id = id,
                        token = token
                    )

                onSuccess(order)

            } catch (e: Exception) {

                onError(
                    e.message
                        ?: "Failed to load your order"
                )
            }
        }
    }

    fun createOrder(
        request: CreateOrderRequest,
        token: String,
        onSuccess: (Order) -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch {

            try {

                val order =
                    repository.createOrder(
                        request = request,
                        token = token
                    )

                onSuccess(order)

            } catch (e: Exception) {

                onError(
                    e.message
                        ?: "Failed to create order"
                )
            }
        }
    }

    fun cancelMyOrder(
        id: Long,
        token: String,
        onSuccess: (Order) -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch {

            try {

                val order =
                    repository.cancelMyOrder(
                        id = id,
                        token = token
                    )

                onSuccess(order)

            } catch (e: Exception) {

                onError(
                    e.message
                        ?: "Failed to cancel order"
                )
            }
        }
    }
}