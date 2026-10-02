package com.mkulimafeeds.data.mapper

import com.mkulimafeeds.data.model.Product
import com.mkulimafeeds.data.remote.ApiConfig
import com.mkulimafeeds.data.remote.ProductResponseDto

fun ProductResponseDto.toProduct(): Product {

    val resolvedImageUrl =
        imageUrl
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { path ->

                if (
                    path.startsWith(
                        "http://",
                        ignoreCase = true
                    ) ||
                    path.startsWith(
                        "https://",
                        ignoreCase = true
                    )
                ) {
                    path
                } else {
                    "${ApiConfig.BASE_URL.trimEnd('/')}/${path.trimStart('/')}"
                }
            }

    return Product(
        id = id.toString(),
        name = name,

        // Backend currently does not expose a SKU.
        sku = "",

        price =
            price
                .toDoubleOrNull()
                ?: 0.0,

        stock =
            stockQuantity
                .toDoubleOrNull()
                ?.toInt()
                ?: 0,

        category = category,

        description =
            description ?: "",

        imageResId = null,

        unit = unit,

        imageUrl = resolvedImageUrl
    )
}