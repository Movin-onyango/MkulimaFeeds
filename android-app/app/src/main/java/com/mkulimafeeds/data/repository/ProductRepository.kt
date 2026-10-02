package com.mkulimafeeds.data.repository

import com.mkulimafeeds.data.mapper.toProduct
import com.mkulimafeeds.data.model.Product
import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.data.remote.CreateProductRequestDto
import com.mkulimafeeds.data.remote.UpdateProductRequestDto

class ProductRepository(
    private val apiService: ApiService
) {

    // ---------------------------------------------------------
    // GET ALL PRODUCTS
    // ---------------------------------------------------------

    suspend fun getProducts(): List<Product> {

        return apiService
            .getProducts()
            .map { it.toProduct() }
    }

    // ---------------------------------------------------------
    // GET PRODUCT BY ID
    // ---------------------------------------------------------

    suspend fun getProduct(
        id: Long
    ): Product {

        return apiService
            .getProduct(id)
            .toProduct()
    }

    // ---------------------------------------------------------
    // CREATE PRODUCT
    // ---------------------------------------------------------

    suspend fun createProduct(
        name: String,
        description: String,
        category: String,
        unit: String,
        price: Double,
        stockQuantity: Int,
        token: String
    ): Product {

        val request = CreateProductRequestDto(
            name = name,
            description = description,
            category = category,
            unit = unit,
            price = price.toString(),
            stockQuantity = stockQuantity.toString()
        )

        return apiService
            .createProduct(
                request = request,
                token = token
            )
            .toProduct()
    }

    // ---------------------------------------------------------
    // UPDATE PRODUCT
    // ---------------------------------------------------------

    suspend fun updateProduct(
        id: Long,
        name: String? = null,
        description: String? = null,
        category: String? = null,
        unit: String? = null,
        price: Double? = null,
        stockQuantity: Int? = null,
        isActive: Boolean? = null,
        token: String
    ): Product {

        val request = UpdateProductRequestDto(
            name = name,
            description = description,
            category = category,
            unit = unit,
            price = price?.toString(),
            stockQuantity = stockQuantity?.toString(),
            isActive = isActive
        )

        return apiService
            .updateProduct(
                id = id,
                request = request,
                token = token
            )
            .toProduct()
    }

    // ---------------------------------------------------------
    // DELETE / DEACTIVATE PRODUCT
    // ---------------------------------------------------------

    suspend fun deleteProduct(
        id: Long,
        token: String
    ) {

        apiService.deleteProduct(
            id = id,
            token = token
        )
    }
    suspend fun uploadProductImage(
        id: Long,
        imageBytes: ByteArray,
        fileName: String,
        mimeType: String,
        token: String
    ) {
        apiService.uploadProductImage(
            id = id,
            imageBytes = imageBytes,
            fileName = fileName,
            mimeType = mimeType,
            token = token
        )
    }
    // ---------------------------------------------------------
    // SEARCH
    // ---------------------------------------------------------

    suspend fun searchProducts(
        query: String
    ): List<Product> {

        val products = getProducts()

        if (query.isBlank()) {
            return products
        }

        return products.filter {

            it.name.contains(
                query,
                ignoreCase = true
            ) ||
                    it.sku.contains(
                        query,
                        ignoreCase = true
                    )
        }
    }
}

