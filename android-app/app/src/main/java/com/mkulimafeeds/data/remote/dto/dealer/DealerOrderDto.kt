package com.mkulimafeeds.data.remote.dto.dealer

import kotlinx.serialization.Serializable

@Serializable
data class DealerOrderResponseDto(
    val id: Long,
    val customerId: Long,
    val customerName: String,
    val telephone: String,
    val location: String,
    val neededDate: Long,
    val status: String,
    val notes: String? = null,
    val totalAmount: String,
    val assignedDealerId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<DealerOrderItemResponseDto>
)

@Serializable
data class DealerOrderItemResponseDto(
    val id: Long,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val quantity: String,
    val unitPrice: String,
    val subtotal: String,
    val createdAt: Long
)

@Serializable
data class DealerUpdateOrderStatusRequest(
    val status: String
)
