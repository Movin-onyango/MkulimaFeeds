package com.mkulimafeeds.data.repository

import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.data.remote.NetworkModule

class AdminStatsRepository(
    private val apiService: ApiService = NetworkModule.apiService
) {

    /**
     * Count of dealer applications awaiting review.
     * Reuses the existing dealer applications endpoint.
     */
    suspend fun getPendingApplicationsCount(token: String): Int {
        return try {
            apiService.getDealerApplications(token).size
        } catch (_: Exception) {
            0
        }
    }
}