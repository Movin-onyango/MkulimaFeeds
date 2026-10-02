package com.movofeeds.models

import kotlinx.serialization.Serializable

@Serializable
data class ProductResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val category: String,
    val unit: String,
    val price: String,
    val stockQuantity: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val imageUrl: String? = null
)

@Serializable
data class CreateProductRequest(
    val name: String,
    val description: String? = null,
    val category: String,
    val unit: String,
    val price: String,
    val stockQuantity: String = "0"
)

@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val unit: String? = null,
    val price: String? = null,
    val stockQuantity: String? = null,
    val isActive: Boolean? = null
)