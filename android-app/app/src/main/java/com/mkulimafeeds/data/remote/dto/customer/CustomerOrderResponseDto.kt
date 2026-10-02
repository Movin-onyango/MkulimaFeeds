package com.mkulimafeeds.data.remote.dto.customer

import kotlinx.serialization.Serializable

@Serializable
data class CustomerOrderResponseDto(
    val id: Long,
    val customerId: Long,
    val telephone: String,
    val location: String,
    val neededDate: Long,
    val status: String,
    val notes: String? = null,
    val totalAmount: String,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<CustomerOrderItemResponseDto>
)

@Serializable
data class CustomerOrderItemResponseDto(
    val id: Long,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val quantity: String,
    val unitPrice: String,
    val subtotal: String,
    val createdAt: Long
)