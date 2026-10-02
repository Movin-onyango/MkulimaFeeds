package com.mkulimafeeds.data.repository.dealer

import com.mkulimafeeds.data.mapper.dealer.toDomain
import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.domain.model.dealer.DealerCustomer

class DealerCustomerRepository(
    private val apiService: ApiService
) {

    suspend fun getCustomers(
        token: String
    ): Result<List<DealerCustomer>> {
        return try {
            val customers = apiService
                .getDealerCustomers(token)
                .map { it.toDomain() }

            Result.success(customers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}