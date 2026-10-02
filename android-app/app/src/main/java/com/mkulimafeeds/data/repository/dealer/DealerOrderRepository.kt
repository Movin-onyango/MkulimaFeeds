package com.mkulimafeeds.data.repository.dealer

import com.mkulimafeeds.data.mapper.dealer.toDomain
import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.domain.model.dealer.DealerOrder

class DealerOrderRepository(
    private val apiService: ApiService
) {


    suspend fun getAssignedOrders(
        token: String
    ): List<DealerOrder> {

        return apiService
            .getDealerOrders(token)
            .map { it.toDomain() }
    }

    suspend fun getAssignedOrder(
        id: Long,
        token: String
    ): DealerOrder {

        return apiService
            .getDealerOrder(
                id = id,
                token = token
            )
            .toDomain()
    }

    suspend fun updateOrderStatus(
        id: Long,
        status: String,
        token: String
    ): DealerOrder {

        return apiService
            .updateDealerOrderStatus(
                id = id,
                status = status,
                token = token
            )
            .toDomain()
    }


}
