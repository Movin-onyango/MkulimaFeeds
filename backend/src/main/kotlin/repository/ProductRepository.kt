package com.movofeeds.repository

import com.movofeeds.database.tables.ProductsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

data class ProductRecord(
    val id: Long,
    val name: String,
    val description: String?,
    val category: String,
    val unit: String,
    val price: BigDecimal,
    val stockQuantity: BigDecimal,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

class ProductRepository {

    fun findById(id: Long): ProductRecord? =
        transaction {
            ProductsTable
                .selectAll()
                .where { ProductsTable.id eq id }
                .singleOrNull()
                ?.toProductRecord()
        }

    fun findAll(): List<ProductRecord> =
        transaction {
            ProductsTable
                .selectAll()
                .map { it.toProductRecord() }
        }

    fun findActive(): List<ProductRecord> =
        transaction {
            ProductsTable
                .selectAll()
                .where { ProductsTable.isActive eq true }
                .map { it.toProductRecord() }
        }

    fun createProduct(
        name: String,
        description: String?,
        category: String,
        unit: String,
        price: BigDecimal,
        stockQuantity: BigDecimal = BigDecimal.ZERO
    ): ProductRecord =
        transaction {

            val now = System.currentTimeMillis()

            val id = ProductsTable
                .insertAndGetId {
                    it[ProductsTable.name] = name
                    it[ProductsTable.description] = description
                    it[ProductsTable.category] = category
                    it[ProductsTable.unit] = unit
                    it[ProductsTable.price] = price
                    it[ProductsTable.stockQuantity] = stockQuantity
                    it[ProductsTable.isActive] = true
                    it[ProductsTable.createdAt] = now
                    it[ProductsTable.updatedAt] = now
                }
                .value

            findById(id)
                ?: error("Failed to retrieve newly created product")
        }

    fun update(
        id: Long,
        name: String?,
        description: String?,
        category: String?,
        unit: String?,
        price: BigDecimal?,
        stockQuantity: BigDecimal?,
        isActive: Boolean?
    ): ProductRecord? =
        transaction {

            val updated = ProductsTable.update(
                where = { ProductsTable.id eq id }
            ) {

                if (name != null) {
                    it[ProductsTable.name] = name
                }

                if (description != null) {
                    it[ProductsTable.description] = description
                }

                if (category != null) {
                    it[ProductsTable.category] = category
                }

                if (unit != null) {
                    it[ProductsTable.unit] = unit
                }

                if (price != null) {
                    it[ProductsTable.price] = price
                }

                if (stockQuantity != null) {
                    it[ProductsTable.stockQuantity] = stockQuantity
                }

                if (isActive != null) {
                    it[ProductsTable.isActive] = isActive
                }

                it[ProductsTable.updatedAt] =
                    System.currentTimeMillis()
            }

            if (updated == 0) {
                null
            } else {
                findById(id)
            }
        }

    fun deactivate(id: Long): Boolean =
        transaction {

            val updated = ProductsTable.update(
                where = { ProductsTable.id eq id }
            ) {
                it[ProductsTable.isActive] = false
                it[ProductsTable.updatedAt] =
                    System.currentTimeMillis()
            }

            updated > 0
        }

    private fun ResultRow.toProductRecord(): ProductRecord =
        ProductRecord(
            id = this[ProductsTable.id].value,
            name = this[ProductsTable.name],
            description = this[ProductsTable.description],
            category = this[ProductsTable.category],
            unit = this[ProductsTable.unit],
            price = this[ProductsTable.price],
            stockQuantity = this[ProductsTable.stockQuantity],
            isActive = this[ProductsTable.isActive],
            createdAt = this[ProductsTable.createdAt],
            updatedAt = this[ProductsTable.updatedAt]
        )
}