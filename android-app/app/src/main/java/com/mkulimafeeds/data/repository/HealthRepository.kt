package com.mkulimafeeds.data.repository
import com.mkulimafeeds.data.remote.ApiService

class HealthRepository(
    private val apiService: ApiService
) {

    suspend fun checkHealth(): Result<String> {
        return runCatching {
            apiService.healthCheck()
        }
    }
}