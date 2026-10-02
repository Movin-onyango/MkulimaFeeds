package com.mkulimafeeds.data.repository
import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.data.remote.DealerResponseDto

class DealerRepository(
    private val apiService: ApiService
) {

    suspend fun getAdminDealers(
        token: String
    ): List<DealerResponseDto> {
        return apiService.getAdminDealers(token)
    }
}