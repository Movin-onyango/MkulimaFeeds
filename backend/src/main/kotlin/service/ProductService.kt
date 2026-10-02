package com.movofeeds.service

import com.movofeeds.repository.ProductRecord
import com.movofeeds.repository.ProductRepository
import java.math.BigDecimal

class ProductService(
    private val productRepository: ProductRepository = ProductRepository()
) {

    fun getAllProducts(): List<ProductRecord> {
        return productRepository.findAll()
    }

    fun getActiveProducts(): List<ProductRecord> {
        return productRepository.findActive()
    }

    fun getProductById(id: Long): ProductRecord {
        require(id > 0) {
            "Invalid product ID"
        }

        return productRepository.findById(id)
            ?: throw NoSuchElementException("Product not found")
    }

    fun createProduct(
        name: String,
        description: String?,
        category: String,
        unit: String,
        price: String,
        stockQuantity: String = "0"
    ): ProductRecord {

        val normalizedName = name.trim()
        val normalizedCategory = category.trim()
        val normalizedUnit = unit.trim()

        require(normalizedName.isNotBlank()) {
            "Product name is required"
        }

        require(normalizedCategory.isNotBlank()) {
            "Product category is required"
        }

        require(normalizedUnit.isNotBlank()) {
            "Product unit is required"
        }

        val productPrice = parseMoney(
            price,
            "Product price"
        )

        val productStock = parseMoney(
            stockQuantity,
            "Stock quantity"
        )

        require(productPrice >= BigDecimal.ZERO) {
            "Product price cannot be negative"
        }

        require(productStock >= BigDecimal.ZERO) {
            "Stock quantity cannot be negative"
        }

        return productRepository.createProduct(
            name = normalizedName,
            description = description?.trim(),
            category = normalizedCategory,
            unit = normalizedUnit,
            price = productPrice,
            stockQuantity = productStock
        )
    }

    fun updateProduct(
        id: Long,
        name: String?,
        description: String?,
        category: String?,
        unit: String?,
        price: String?,
        stockQuantity: String?,
        isActive: Boolean?
    ): ProductRecord {

        require(id > 0) {
            "Invalid product ID"
        }

        // Make sure the product exists first.
        productRepository.findById(id)
            ?: throw NoSuchElementException("Product not found")

        val normalizedName = name?.trim()
        val normalizedCategory = category?.trim()
        val normalizedUnit = unit?.trim()

        if (normalizedName != null) {
            require(normalizedName.isNotBlank()) {
                "Product name cannot be empty"
            }
        }

        if (normalizedCategory != null) {
            require(normalizedCategory.isNotBlank()) {
                "Product category cannot be empty"
            }
        }

        if (normalizedUnit != null) {
            require(normalizedUnit.isNotBlank()) {
                "Product unit cannot be empty"
            }
        }

        val productPrice = price?.let {
            parseMoney(it, "Product price")
        }

        val productStock = stockQuantity?.let {
            parseMoney(it, "Stock quantity")
        }

        if (productPrice != null) {
            require(productPrice >= BigDecimal.ZERO) {
                "Product price cannot be negative"
            }
        }

        if (productStock != null) {
            require(productStock >= BigDecimal.ZERO) {
                "Stock quantity cannot be negative"
            }
        }

        return productRepository.update(
            id = id,
            name = normalizedName,
            description = description?.trim(),
            category = normalizedCategory,
            unit = normalizedUnit,
            price = productPrice,
            stockQuantity = productStock,
            isActive = isActive
        ) ?: throw NoSuchElementException(
            "Product not found"
        )
    }

    fun deactivateProduct(id: Long) {
        require(id > 0) {
            "Invalid product ID"
        }

        val deactivated = productRepository.deactivate(id)

        if (!deactivated) {
            throw NoSuchElementException(
                "Product not found"
            )
        }
    }

    private fun parseMoney(
        value: String,
        fieldName: String
    ): BigDecimal {
        return try {
            BigDecimal(value.trim())
        } catch (_: NumberFormatException) {
            throw IllegalArgumentException(
                "$fieldName must be a valid number"
            )
        }
    }
}
