package com.mkulimafeeds.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderRequest(
    val telephone: String,
    val location: String,
    val neededDate: Long,
    val notes: String? = null,
    val items: List<CreateOrderItemRequest>
)

@Serializable
data class CreateOrderItemRequest(
    val productId: Long,
    val quantity: String
)

@Serializable
data class CancelOrderResponse(
    val message: String
)