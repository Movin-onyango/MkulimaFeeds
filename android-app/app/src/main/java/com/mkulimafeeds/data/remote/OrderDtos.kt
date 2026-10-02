package com.mkulimafeeds.data.remote
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponseDto(
    val id: Long,
    val customerId: Long,
    val customerName: String,
    val assignedDealerId: Long?,
    val telephone: String,
    val location: String,
    val neededDate: Long,
    val status: String,
    val notes: String? = null,
    val totalAmount: String,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<OrderItemResponseDto>
)

@Serializable
data class OrderItemResponseDto(
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
data class UpdateOrderStatusRequest(
    val status: String
)