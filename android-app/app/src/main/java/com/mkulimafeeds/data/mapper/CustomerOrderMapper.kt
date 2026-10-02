package com.mkulimafeeds.data.mapper

import com.mkulimafeeds.data.remote.dto.customer.CustomerOrderResponseDto
import com.mkulimafeeds.domain.model.CustomerOrder
import com.mkulimafeeds.domain.model.CustomerOrderItem
import com.mkulimafeeds.domain.model.CustomerOrderStatus
import java.math.BigDecimal

fun CustomerOrderResponseDto.toDomain(): CustomerOrder {
    return CustomerOrder(
        id = id,
        telephone = telephone.trim(),
        location = location.trim(),
        neededDate = neededDate,
        status = status.toCustomerOrderStatus(),
        notes = notes?.trim()?.takeIf { it.isNotEmpty() },
        totalAmount = totalAmount.toMoney("totalAmount"),
        createdAt = createdAt,
        updatedAt = updatedAt,
        items = items.map { it.toDomain() }
    )
}

private fun com.mkulimafeeds.data.remote.dto.customer.CustomerOrderItemResponseDto.toDomain():
        CustomerOrderItem {

    return CustomerOrderItem(
        id = id,
        orderId = orderId,
        productId = productId,
        productName = productName.trim(),
        quantity = quantity.toDecimal("quantity"),
        unitPrice = unitPrice.toMoney("unitPrice"),
        subtotal = subtotal.toMoney("subtotal"),
        createdAt = createdAt
    )
}

private fun String.toMoney(fieldName: String): BigDecimal {
    return trim()
        .toBigDecimalOrNull()
        ?: throw IllegalArgumentException(
            "Invalid monetary value for $fieldName: '$this'"
        )
}

private fun String.toDecimal(fieldName: String): BigDecimal {
    return trim()
        .toBigDecimalOrNull()
        ?.takeIf { it >= BigDecimal.ZERO }
        ?: throw IllegalArgumentException(
            "Invalid quantity for $fieldName: '$this'"
        )
}

private fun String.toCustomerOrderStatus(): CustomerOrderStatus {
    return when (trim().uppercase()) {
        "PENDING" -> CustomerOrderStatus.PENDING
        "CONFIRMED" -> CustomerOrderStatus.CONFIRMED
        "PROCESSING" -> CustomerOrderStatus.PROCESSING
        "READY" -> CustomerOrderStatus.READY
        "COMPLETED" -> CustomerOrderStatus.COMPLETED
        "CANCELLED" -> CustomerOrderStatus.CANCELLED
        else -> CustomerOrderStatus.UNKNOWN
    }
}