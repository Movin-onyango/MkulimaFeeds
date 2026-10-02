package com.mkulimafeeds.data.remote
import kotlinx.serialization.Serializable

@Serializable
data class DealerResponseDto(
    val id: Long,
    val name: String,
    val phone: String,
    val email: String,
    val role: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)