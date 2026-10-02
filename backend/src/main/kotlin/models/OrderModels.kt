package com.movofeeds.models
import kotlinx.serialization.Serializable

// ==========================================
// CREATE ORDER
// ==========================================

@Serializable
data class CreateOrderRequest(
    val telephone: String,
    val location: String,
    val neededDate: Long,
    val notes: String? = null,
    val items: List<CreateOrderItemRequest>
)
// =========================================================
// BULK ORDERS
// =========================================================

@Serializable
data class BulkOrderRequest(
    val telephone: String,
    val location: String,
    val neededDate: Long,
    val notes: String? = null,
    val items: List<CreateOrderItemRequest>
)

@Serializable
data class BulkOrderValidationResponse(
    val valid: Boolean,
    val errors: List<String>,
    val minimumOrderValue: String,
    val minimumQtyPerItem: Int,
    val maximumQtyPerItem: Int,
    val leadTimeDays: Int,
    val paymentTerms: String
)
@Serializable
data class AssignDealerRequest(
    val dealerId: Long?
)
@Serializable
data class CreateOrderItemRequest(
    val productId: Long,
    val quantity: String
)

// ==========================================
// ORDER RESPONSE
// ==========================================

@Serializable
data class OrderResponse(
    val id: Long,
    val customerId: Long,
    val customerName: String,
    val assignedDealerId: Long?,
    val telephone: String,
    val location: String,
    val neededDate: Long,
    val status: String,
    val notes: String?,
    val totalAmount: String,
    val createdAt: Long,
    val orderType: String,
    val updatedAt: Long,
    val items: List<OrderItemResponse>
)

@Serializable
data class OrderItemResponse(
    val id: Long,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val quantity: String,
    val unitPrice: String,
    val subtotal: String,
    val createdAt: Long
)