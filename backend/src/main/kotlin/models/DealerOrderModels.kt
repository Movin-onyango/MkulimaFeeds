package com.movofeeds.models

import kotlinx.serialization.Serializable

@Serializable
data class DealerOrderResponse(
    val id: Long,
    val customerId: Long,
    val customerName: String,
    val telephone: String,
    val location: String,
    val neededDate: Long,
    val status: String,
    val notes: String?,
    val totalAmount: String,
    val assignedDealerId: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<DealerOrderItemResponse>
)

@Serializable
data class DealerOrderItemResponse(
    val id: Long,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val quantity: String,
    val unitPrice: String,
    val subtotal: String,
    val createdAt: Long
)