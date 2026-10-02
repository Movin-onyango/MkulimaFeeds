package com.mkulimafeeds.data.mapper.dealer

import com.mkulimafeeds.data.remote.dto.dealer.DealerCustomerDto
import com.mkulimafeeds.domain.model.dealer.DealerCustomer

fun DealerCustomerDto.toDomain(): DealerCustomer {
    return DealerCustomer(
        id = id,
        name = name,
        phone = phone,
        email = email,
        totalOrders = totalOrders,
        activeOrders = activeOrders,
        completedOrders = completedOrders
    )
}