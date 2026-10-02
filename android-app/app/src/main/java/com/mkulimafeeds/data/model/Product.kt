package com.mkulimafeeds.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val sku: String,
    val price: Double,
    val stock: Int,
    val category: String,
    val description: String,
    val imageResId: Int? = null,
    val unit: String = "units",
    val imageUrl: String? = null
)