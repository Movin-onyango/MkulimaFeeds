package com.mkulimafeeds.data.repository

import com.mkulimafeeds.Order
import com.mkulimafeeds.data.mapper.toOrder
import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.data.remote.CreateOrderRequest
import com.mkulimafeeds.data.remote.OrderResponseDto

class OrderRepository(
    private val apiService: ApiService
) {

    // =========================================================
    // ADMIN ORDERS
    // =========================================================

    suspend fun getAdminOrders(
        token: String
    ): List<Order> {
        return apiService
            .getAdminOrders(token)
            .map { it.toOrder() }
    }

    suspend fun getAdminOrder(
        id: Long,
        token: String
    ): Order {
        return apiService
            .getAdminOrder(id, token)
            .toOrder()
    }

    suspend fun updateOrderStatus(
        id: Long,
        status: String,
        token: String
    ): Order {
        return apiService
            .updateOrderStatus(
                id = id,
                status = status,
                token = token
            )
            .toOrder()
    }

    suspend fun assignDealer(
        id: Long,
        dealerId: Long?,
        token: String
    ): Order {
        return apiService
            .assignDealer(
                id = id,
                dealerId = dealerId,
                token = token
            )
            .toOrder()
    }

    // =========================================================
    // CUSTOMER ORDERS
    // =========================================================

    suspend fun createOrder(
        request: CreateOrderRequest,
        token: String
    ): Order {
        return apiService
            .createOrder(
                request = request,
                token = token
            )
            .toOrder()
    }

    suspend fun getMyOrders(
        token: String
    ): List<Order> {
        return apiService
            .getMyOrders(token)
            .map { it.toOrder() }
    }

    suspend fun getMyOrder(
        id: Long,
        token: String
    ): Order {
        return apiService
            .getMyOrder(
                id = id,
                token = token
            )
            .toOrder()
    }

    suspend fun cancelMyOrder(
        id: Long,
        token: String
    ): Order {
        return apiService
            .cancelMyOrder(
                id = id,
                token = token
            )
            .toOrder()
    }
}