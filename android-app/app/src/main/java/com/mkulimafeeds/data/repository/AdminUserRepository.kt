package com.mkulimafeeds.data.repository

import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.data.remote.dto.AdminUserDto
import com.mkulimafeeds.data.remote.dto.CreateAdminUserRequestDto
import com.mkulimafeeds.data.remote.dto.UpdateAdminUserRequestDto

class AdminUserRepository(
    private val apiService: ApiService
) {

    suspend fun getAdminUsers(
        token: String
    ): List<AdminUserDto> {
        return apiService.getAdminUsers(token)
    }

    suspend fun createAdminUser(
        request: CreateAdminUserRequestDto,
        token: String
    ): AdminUserDto {
        return apiService.createAdminUser(
            request,
            token
        )
    }

    suspend fun updateAdminUser(
        id: Long,
        request: UpdateAdminUserRequestDto,
        token: String
    ): AdminUserDto {
        return apiService.updateAdminUser(
            id,
            request,
            token
        )
    }

    suspend fun deactivateAdminUser(
        id: Long,
        token: String
    ) {
        apiService.deactivateAdminUser(
            id,
            token
        )
    }
}