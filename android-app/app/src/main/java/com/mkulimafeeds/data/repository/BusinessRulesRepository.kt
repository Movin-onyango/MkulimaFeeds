package com.mkulimafeeds.data.repository

import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.remote.SettingResponse
import com.mkulimafeeds.data.remote.SettingUpdate
import com.mkulimafeeds.data.remote.UpdateSettingsResponse

class BusinessRulesRepository(
    private val apiService: ApiService = NetworkModule.apiService
) {

    suspend fun getAllSettings(token: String): List<SettingResponse> =
        apiService.getAllSettings(token)

    suspend fun updateSettings(
        changes: List<SettingUpdate>,
        token: String
    ): UpdateSettingsResponse =
        apiService.updateSettings(changes, token)
    suspend fun resetToDefaults(token: String) {
        apiService.resetSettingsToDefaults(token)
    }
}