package com.mkulimafeeds.data.repository.customer

import com.mkulimafeeds.data.mapper.toDomain
import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.domain.model.CustomerOrder

class CustomerOrderRepository(
    private val apiService: ApiService
) {

    suspend fun getMyOrders(token: String): List<CustomerOrder> {
        return apiService
            .getCustomerOrders(token)
            .map { it.toDomain() }
    }

    suspend fun getMyOrder(
        id: Long,
        token: String
    ): CustomerOrder {
        return apiService
            .getCustomerOrder(id, token)
            .toDomain()
    }

    suspend fun cancelOrder(
        id: Long,
        token: String
    ): CustomerOrder {
        return apiService
            .cancelCustomerOrder(id, token)
            .toDomain()
    }
}