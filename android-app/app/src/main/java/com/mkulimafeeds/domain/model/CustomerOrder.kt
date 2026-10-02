package com.mkulimafeeds.domain.model

import java.math.BigDecimal

data class CustomerOrder(
    val id: Long,
    val telephone: String,
    val location: String,
    val neededDate: Long,
    val status: CustomerOrderStatus,
    val notes: String?,
    val totalAmount: BigDecimal,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<CustomerOrderItem>
)

data class CustomerOrderItem(
    val id: Long,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val subtotal: BigDecimal,
    val createdAt: Long
)

enum class CustomerOrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    READY,
    COMPLETED,
    CANCELLED,
    UNKNOWN
}