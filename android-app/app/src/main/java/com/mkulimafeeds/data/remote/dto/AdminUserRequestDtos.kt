package com.mkulimafeeds.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateAdminUserRequestDto(
    val name: String,
    val phone: String,
    val email: String,
    val password: String,
    val role: String = "CUSTOMER"
)

@Serializable
data class UpdateAdminUserRequestDto(
    val name: String,
    val phone: String,
    val email: String
)