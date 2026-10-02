package com.mkulimafeeds.data.repository

import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.data.remote.BrandingSettings
import com.mkulimafeeds.data.remote.NetworkModule

class BrandingRepository(
    private val apiService: ApiService = NetworkModule.apiService
) {

    /**
     * Session-level cache.
     * Branding rarely changes; no point hitting the API on every screen.
     */
    private var cache: BrandingSettings? = null

    suspend fun getBranding(): BrandingSettings {
        cache?.let { return it }

        return try {
            val map = apiService.getPublicBranding()
            val settings = BrandingSettings.fromMap(map)
            cache = settings
            settings
        } catch (_: Exception) {
            BrandingSettings.FALLBACK
        }
    }

    fun clearCache() {
        cache = null
    }
}