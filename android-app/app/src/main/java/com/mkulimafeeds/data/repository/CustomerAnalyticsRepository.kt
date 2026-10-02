package com.mkulimafeeds.data.repository

import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.data.remote.CustomerAnalyticsResponse
import com.mkulimafeeds.data.remote.NetworkModule

class CustomerAnalyticsRepository(
    private val apiService: ApiService = NetworkModule.apiService
) {

    suspend fun getAnalytics(
        token: String
    ): CustomerAnalyticsResponse {
        return apiService.getCustomerAnalytics(token)
    }
}