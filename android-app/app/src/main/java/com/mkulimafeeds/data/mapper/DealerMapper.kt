package com.mkulimafeeds.data.mapper

import com.mkulimafeeds.Dealer
import com.mkulimafeeds.data.remote.DealerResponseDto

fun DealerResponseDto.toDealer(): Dealer {
    return Dealer(
        id = id.toString(),
        name = name,
        location = "Not specified",
        phone = phone,
        rating = 0f,
        status = if (isActive) {
            "ACTIVE"
        } else {
            "INACTIVE"
        }
    )
}