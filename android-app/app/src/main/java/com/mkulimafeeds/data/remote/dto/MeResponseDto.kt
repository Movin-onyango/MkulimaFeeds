/*package com.mkulimafeeds.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MeResponseDto(
    val status: String,
    val user: AdminUserDto
)*/
package com.mkulimafeeds.data.remote.dto

import com.mkulimafeeds.data.remote.UserResponse
import kotlinx.serialization.Serializable

@Serializable
data class MeResponseDto(
    val status: String,
    val user: UserResponse
)