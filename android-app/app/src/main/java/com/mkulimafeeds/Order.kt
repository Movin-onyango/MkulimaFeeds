package com.mkulimafeeds

data class Order(
    val id: String,
    val customerName: String,
    val customerPhone: String,
    val date: String,
    val neededDate: String,
    val totalAmount: Double,
    var status: String,
    val deliveryAddress: String,
    val items: List<OrderItem>,
    var assignedDealerId: String? = null
)

data class OrderItem(
    val name: String,
    val qty: Int,
    val price: Double,
    val subtotal: Double


)