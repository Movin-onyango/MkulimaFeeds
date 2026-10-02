package com.movofeeds.models

import kotlinx.serialization.Serializable

@Serializable
data class AdminUserResponse(
    val id: Long,
    val name: String,
    val phone: String,
    val email: String,
    val role: String,
    val status: String,
    val dealerStatus: String?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val totalOrders: Int
)

@Serializable
data class CreateAdminUserRequest(
    val name: String,
    val phone: String,
    val email: String,
    val password: String,
    val role: String = "CUSTOMER"
)

@Serializable
data class UpdateAdminUserRequest(
    val name: String,
    val phone: String,
    val email: String
)