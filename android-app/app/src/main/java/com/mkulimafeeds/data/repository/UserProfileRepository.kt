package com.mkulimafeeds.data.repository

import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.data.remote.CreateSavedLocationRequest
import com.mkulimafeeds.data.remote.NetworkModule
import com.mkulimafeeds.data.remote.SavedLocationResponse
import com.mkulimafeeds.data.remote.UpdateProfileRequest
import com.mkulimafeeds.data.remote.UpdateSavedLocationRequest
import com.mkulimafeeds.data.remote.UserProfileResponse

class UserProfileRepository(
    private val apiService: ApiService = NetworkModule.apiService
) {

    suspend fun getMyProfile(token: String): UserProfileResponse =
        apiService.getMyProfile(token)

    suspend fun updateMyProfile(
        name: String,
        email: String,
        phone: String,
        token: String
    ): UserProfileResponse =
        apiService.updateMyProfile(
            request = UpdateProfileRequest(
                name = name,
                email = email,
                phone = phone
            ),
            token = token
        )

    suspend fun deleteMyAccount(
        password: String,
        confirmation: String,
        token: String
    ) = apiService.deleteMyAccount(
        password = password,
        confirmation = confirmation,
        token = token
    )

    // ---------------------------------------------------------
    // Saved locations
    // ---------------------------------------------------------

    suspend fun getSavedLocations(
        token: String
    ): List<SavedLocationResponse> =
        apiService.getMySavedLocations(token)

    suspend fun createSavedLocation(
        label: String,
        address: String,
        isDefault: Boolean,
        token: String
    ): SavedLocationResponse =
        apiService.createSavedLocation(
            request = CreateSavedLocationRequest(
                label = label,
                address = address,
                isDefault = isDefault
            ),
            token = token
        )

    suspend fun updateSavedLocation(
        id: Long,
        label: String,
        address: String,
        isDefault: Boolean,
        token: String
    ): SavedLocationResponse =
        apiService.updateSavedLocation(
            id = id,
            request = UpdateSavedLocationRequest(
                label = label,
                address = address,
                isDefault = isDefault
            ),
            token = token
        )

    suspend fun deleteSavedLocation(
        id: Long,
        token: String
    ) = apiService.deleteSavedLocation(id, token)
}