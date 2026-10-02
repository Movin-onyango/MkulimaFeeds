package com.mkulimafeeds.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminUserDto(
    val id: Long,
    val name: String,
    val phone: String,
    val email: String,
    val role: String,
    val status: String = "ACTIVE",
    val dealerStatus: String? = null,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val totalOrders: Int
)