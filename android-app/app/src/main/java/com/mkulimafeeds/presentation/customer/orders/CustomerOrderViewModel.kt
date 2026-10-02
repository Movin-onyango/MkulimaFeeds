package com.mkulimafeeds.presentation.customer.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkulimafeeds.data.repository.customer.CustomerOrderRepository
import com.mkulimafeeds.domain.model.CustomerOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomerOrderUiState(
    val orders: List<CustomerOrder> = emptyList(),
    val selectedOrder: CustomerOrder? = null,
    val isLoading: Boolean = false,
    val isLoadingDetails: Boolean = false,
    val isCancelling: Boolean = false,
    val error: String? = null
)

class CustomerOrderViewModel(
    private val repository: CustomerOrderRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(CustomerOrderUiState())

    val uiState: StateFlow<CustomerOrderUiState> =
        _uiState.asStateFlow()

    fun loadOrders(token: String) {

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
                        error = e.message
                            ?: "Failed to load orders"
                    )
            }
        }
    }

    fun loadOrder(
        id: Long,
        token: String
    ) {

        if (id <= 0L) {
            _uiState.value =
                _uiState.value.copy(
                    isLoadingDetails = false,
                    error = "Invalid order ID"
                )
            return
        }

        if (token.isBlank()) {
            _uiState.value =
                _uiState.value.copy(
                    isLoadingDetails = false,
                    error = "Authentication token not found"
                )
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    selectedOrder = null,
                    isLoadingDetails = true,
                    error = null
                )

            try {

                val order =
                    repository.getMyOrder(
                        id = id,
                        token = token
                    )

                _uiState.value =
                    _uiState.value.copy(
                        selectedOrder = order,
                        isLoadingDetails = false,
                        error = null
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        selectedOrder = null,
                        isLoadingDetails = false,
                        error = e.message
                            ?: "Failed to load order"
                    )
            }
        }
    }

    fun cancelOrder(
        id: Long,
        token: String
    ) {

        if (id <= 0L) {
            _uiState.value =
                _uiState.value.copy(
                    isCancelling = false,
                    error = "Invalid order ID"
                )
            return
        }

        if (token.isBlank()) {
            _uiState.value =
                _uiState.value.copy(
                    isCancelling = false,
                    error = "Authentication token not found"
                )
            return
        }

        if (_uiState.value.isCancelling) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isCancelling = true,
                    error = null
                )

            try {

                val cancelledOrder =
                    repository.cancelOrder(
                        id = id,
                        token = token
                    )

                val updatedOrders =
                    _uiState.value.orders.map { order ->
                        if (order.id == cancelledOrder.id) {
                            cancelledOrder
                        } else {
                            order
                        }
                    }

                _uiState.value =
                    _uiState.value.copy(
                        orders = updatedOrders,
                        selectedOrder = cancelledOrder,
                        isCancelling = false,
                        error = null
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isCancelling = false,
                        error = e.message
                            ?: "Failed to cancel order"
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