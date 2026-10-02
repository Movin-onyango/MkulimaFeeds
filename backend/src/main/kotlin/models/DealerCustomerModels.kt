package com.movofeeds.models

import kotlinx.serialization.Serializable

@Serializable
data class DealerCustomerResponse(
    val id: Long,
    val name: String,
    val phone: String,
    val email: String?,
    val totalOrders: Int,
    val activeOrders: Int,
    val completedOrders: Int
)