package com.mkulimafeeds.domain.model.dealer

import java.math.BigDecimal

data class DealerOrder(
    val id: Long,
    val customerId: Long,
    val customerName: String,
    val telephone: String,
    val location: String,
    val neededDate: Long,
    val status: DealerOrderStatus,
    val notes: String?,
    val totalAmount: BigDecimal,
    val assignedDealerId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<DealerOrderItem>
)

data class DealerOrderItem(
    val id: Long,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val subtotal: BigDecimal,
    val createdAt: Long
)

enum class DealerOrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    READY,
    COMPLETED,
    CANCELLED;


    companion object {
        fun fromApi(value: String): DealerOrderStatus {
            return entries.firstOrNull {
                it.name.equals(value.trim(), ignoreCase = true)
            } ?: throw IllegalArgumentException(
                "Unknown dealer order status: $value"
            )
        }
    }


}
