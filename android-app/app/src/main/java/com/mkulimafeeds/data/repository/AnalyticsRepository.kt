package com.mkulimafeeds.data.repository

import com.mkulimafeeds.data.remote.AnalyticsOverviewResponse
import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.data.remote.NetworkModule

class AnalyticsRepository(
    private val apiService: ApiService = NetworkModule.apiService
) {

    suspend fun getOverview(
        token: String
    ): AnalyticsOverviewResponse {
        return apiService.getAdminAnalytics(token)
    }

}
