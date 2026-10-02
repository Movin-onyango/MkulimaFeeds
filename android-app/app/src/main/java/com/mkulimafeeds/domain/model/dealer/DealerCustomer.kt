package com.mkulimafeeds.domain.model.dealer

data class DealerCustomer(
    val id: Long,
    val name: String,
    val phone: String,
    val email: String?,
    val totalOrders: Int,
    val activeOrders: Int,
    val completedOrders: Int
)