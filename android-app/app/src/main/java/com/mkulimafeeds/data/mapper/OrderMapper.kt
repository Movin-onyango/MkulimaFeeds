package com.mkulimafeeds.data.mapper

import com.mkulimafeeds.Order
import com.mkulimafeeds.OrderItem
import com.mkulimafeeds.data.remote.OrderResponseDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun OrderResponseDto.toOrder(): Order {

    val dateFormatter = SimpleDateFormat(
        "MMM dd, yyyy",
        Locale.getDefault()
    )

    val formattedDate = dateFormatter.format(Date(createdAt))
    val formattedNeededDate = dateFormatter.format(Date(neededDate))
    return Order(
        id = id.toString(),
        customerName = customerName,
        customerPhone = telephone,
        assignedDealerId = assignedDealerId?.toString(),
        date = formattedDate,
        neededDate = formattedNeededDate,
        totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
        status = status,
        deliveryAddress = location,
        items = items.map { item ->
            OrderItem(
                name = item.productName,
                qty = item.quantity.toDoubleOrNull()?.toInt() ?: 0,
                price = item.unitPrice.toDoubleOrNull() ?: 0.0,
                subtotal = item.subtotal.toDoubleOrNull() ?: 0.0
            )
        }
    )
}