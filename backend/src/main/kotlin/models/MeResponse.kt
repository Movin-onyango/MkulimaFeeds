package com.movofeeds.models

import kotlinx.serialization.Serializable

@Serializable
data class MeResponse(
    val status: String,
    val user: UserResponse
)