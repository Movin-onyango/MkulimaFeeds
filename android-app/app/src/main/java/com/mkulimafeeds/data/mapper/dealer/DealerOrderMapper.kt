package com.mkulimafeeds.data.mapper.dealer

import com.mkulimafeeds.data.remote.dto.dealer.DealerOrderResponseDto
import com.mkulimafeeds.domain.model.dealer.DealerOrder
import com.mkulimafeeds.domain.model.dealer.DealerOrderItem
import java.math.BigDecimal

fun DealerOrderResponseDto.toDomain(): DealerOrder {
    return DealerOrder(
        id = id,
        customerId = customerId,
        customerName = customerName,
        telephone = telephone,
        location = location,
        neededDate = neededDate,
        status = com.mkulimafeeds.domain.model.dealer.DealerOrderStatus.fromApi(status),
        notes = notes,
        totalAmount = totalAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        assignedDealerId = assignedDealerId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        items = items.map { it.toDomain() }
    )
}

private fun com.mkulimafeeds.data.remote.dto.dealer.DealerOrderItemResponseDto.toDomain(): DealerOrderItem {
    return DealerOrderItem(
        id = id,
        orderId = orderId,
        productId = productId,
        productName = productName,
        quantity = quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        unitPrice = unitPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        subtotal = subtotal.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        createdAt = createdAt
    )
}
