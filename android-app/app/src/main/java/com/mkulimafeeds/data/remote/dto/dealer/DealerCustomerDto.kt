package com.mkulimafeeds.data.remote.dto.dealer

import kotlinx.serialization.Serializable

@Serializable
data class DealerCustomerDto(
    val id: Long,
    val name: String,
    val phone: String,
    val email: String? = null,
    val totalOrders: Int,
    val activeOrders: Int,
    val completedOrders: Int
)