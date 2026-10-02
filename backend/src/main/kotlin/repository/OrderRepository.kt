package com.movofeeds.repository

import com.movofeeds.database.tables.OrderItemsTable
import com.movofeeds.database.tables.OrdersTable
import com.movofeeds.database.tables.ProductsTable
import com.movofeeds.database.tables.UsersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

data class OrderItemRecord(
    val id: Long,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val subtotal: BigDecimal,
    val createdAt: Long
)

data class OrderRecord(
    val id: Long,
    val customerId: Long,
    val assignedDealerId: Long?,
    val telephone: String,
    val location: String,
    val orderType: String,
    val neededDate: Long,
    val status: String,
    val notes: String?,
    val totalAmount: BigDecimal,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<OrderItemRecord>
)

class OrderRepository {

    // ==========================================
    // CREATE ORDER
    // ==========================================

    fun createOrder(
        customerId: Long,
        telephone: String,
        location: String,
        neededDate: Long,
        notes: String?,
        items: List<CreateOrderItemRecord>,
        totalAmount: BigDecimal,
        orderType: String = "RETAIL"
    ): OrderRecord =
        transaction {
            val now = System.currentTimeMillis()

            val orderId = OrdersTable
                .insertAndGetId {
                    it[OrdersTable.customerId] = customerId
                    it[OrdersTable.telephone] = telephone
                    it[OrdersTable.location] = location
                    it[OrdersTable.neededDate] = neededDate
                    it[OrdersTable.status] = "PENDING"
                    it[OrdersTable.notes] = notes
                    it[OrdersTable.totalAmount] = totalAmount
                    it[OrdersTable.orderType] = orderType
                    it[OrdersTable.createdAt] = now
                    it[OrdersTable.updatedAt] = now
                }
                .value

            items.forEach { item ->
                OrderItemsTable.insert {
                    it[OrderItemsTable.orderId] = orderId
                    it[OrderItemsTable.productId] = item.productId
                    it[OrderItemsTable.quantity] = item.quantity
                    it[OrderItemsTable.unitPrice] = item.unitPrice
                    it[OrderItemsTable.subtotal] = item.subtotal
                    it[OrderItemsTable.createdAt] = now
                }
            }

            findById(orderId)
                ?: error("Failed to retrieve newly created order")
        }

    // ==========================================
    // FIND BY ID
    // ==========================================

    fun findById(id: Long): OrderRecord? =
        transaction {
            val order = OrdersTable
                .selectAll()
                .where { OrdersTable.id eq id }
                .singleOrNull()
                ?: return@transaction null

            val items = findItemsByOrderId(id)
            order.toOrderRecord(items)
        }

    // ==========================================
    // FIND CUSTOMER ORDERS
    // ==========================================

    fun findByCustomerId(customerId: Long): List<OrderRecord> =
        transaction {
            OrdersTable
                .selectAll()
                .where { OrdersTable.customerId eq customerId }
                .orderBy(OrdersTable.createdAt, SortOrder.DESC)
                .map { order ->
                    val orderId = order[OrdersTable.id].value
                    order.toOrderRecord(findItemsByOrderId(orderId))
                }
        }

    fun findByAssignedDealerId(dealerId: Long): List<OrderRecord> =
        transaction {
            OrdersTable
                .selectAll()
                .where { OrdersTable.assignedDealerId eq dealerId }
                .orderBy(OrdersTable.createdAt, SortOrder.DESC)
                .map { order ->
                    val orderId = order[OrdersTable.id].value
                    order.toOrderRecord(findItemsByOrderId(orderId))
                }
        }

    // ==========================================
    // FIND ALL ORDERS
    // ==========================================

    fun findAll(): List<OrderRecord> =
        transaction {
            OrdersTable
                .selectAll()
                .orderBy(OrdersTable.createdAt, SortOrder.DESC)
                .map { order ->
                    val orderId = order[OrdersTable.id].value
                    order.toOrderRecord(findItemsByOrderId(orderId))
                }
        }

    // ==========================================
    // COUNT COMPLETED ORDERS BY CUSTOMER
    //
    // Only counts orders with status = "COMPLETED".
    // This is the customer's "real" order count —
    // cancelled or pending orders don't reflect
    // the customer's value.
    // ==========================================

    fun countByCustomerId(customerId: Long): Int =
        transaction {
            OrdersTable
                .selectAll()
                .where {
                    (OrdersTable.customerId eq customerId) and
                            (OrdersTable.status eq "COMPLETED")
                }
                .count()
                .toInt()
        }
    /**
     * Count ALL orders for a customer regardless of status.
     * Used by the profile screen's "total orders" stat.
     */
    fun countAllOrdersByCustomerId(customerId: Long): Int =
        transaction {
            OrdersTable
                .selectAll()
                .where {
                    OrdersTable.customerId eq customerId
                }
                .count()
                .toInt()
        }

    // ==========================================
    // SUM COMPLETED REVENUE BY CUSTOMER
    //
    // Returns the total amount the customer has
    // spent on COMPLETED orders. Used for the
    // "Top customers by revenue" leaderboard.
    // ==========================================

    fun sumCompletedRevenueByCustomerId(
        customerId: Long
    ): BigDecimal =
        transaction {
            OrdersTable
                .selectAll()
                .where {
                    (OrdersTable.customerId eq customerId) and
                            (OrdersTable.status eq "COMPLETED")
                }
                .fold(BigDecimal.ZERO) { total, row ->
                    total.add(row[OrdersTable.totalAmount])
                }
        }

    // ==========================================
    // COUNT COMPLETED ORDERS IN DATE RANGE
    //
    // Used for signup-trend computations and any
    // future cohort analysis.
    // ==========================================

    fun countCompletedOrdersInRange(
        startMs: Long,
        endMs: Long
    ): Int =
        transaction {
            OrdersTable
                .selectAll()
                .where {
                    (OrdersTable.status eq "COMPLETED") and
                            (OrdersTable.createdAt greaterEq startMs) and
                            (OrdersTable.createdAt less endMs)
                }
                .count()
                .toInt()
        }

    // ==========================================
    // UPDATE STATUS + INVENTORY
    // ==========================================

    fun updateStatus(
        id: Long,
        status: String
    ): OrderRecord? =
        transaction {
            val order = OrdersTable
                .selectAll()
                .where { OrdersTable.id eq id }
                .singleOrNull()
                ?: return@transaction null

            val currentStatus = order[OrdersTable.status]

            // ------------------------------------------
            // Validate status transition
            // ------------------------------------------
            val allowedTransitions = mapOf(
                "PENDING" to setOf("CONFIRMED", "CANCELLED"),
                "CONFIRMED" to setOf("PROCESSING", "CANCELLED"),
                "PROCESSING" to setOf("READY", "CANCELLED"),
                "READY" to setOf("COMPLETED"),
                "COMPLETED" to emptySet(),
                "CANCELLED" to emptySet()
            )

            val allowedNextStatuses =
                allowedTransitions[currentStatus] ?: emptySet()

            require(status in allowedNextStatuses) {
                "Invalid order status transition: $currentStatus -> $status"
            }

            val orderItems = findItemsByOrderId(id)

            // ------------------------------------------
            // CONFIRM ORDER — deduct inventory
            // ------------------------------------------
            if (currentStatus != "CONFIRMED" && status == "CONFIRMED") {
                orderItems.forEach { item ->
                    val product = ProductsTable
                        .selectAll()
                        .where { ProductsTable.id eq item.productId }
                        .singleOrNull()
                        ?: error("Product ${item.productId} not found")

                    val currentStock = product[ProductsTable.stockQuantity]
                    require(currentStock >= item.quantity) {
                        "Insufficient stock for ${product[ProductsTable.name]}. " +
                                "Available: $currentStock, requested: ${item.quantity}"
                    }

                    val newStock = currentStock.subtract(item.quantity)

                    ProductsTable.update(
                        where = { ProductsTable.id eq item.productId }
                    ) {
                        it[ProductsTable.stockQuantity] = newStock
                        it[ProductsTable.updatedAt] = System.currentTimeMillis()
                    }
                }
            }

            // ------------------------------------------
            // CANCEL CONFIRMED / PROCESSING — restore inventory
            // ------------------------------------------
            if (status == "CANCELLED" &&
                (currentStatus == "CONFIRMED" || currentStatus == "PROCESSING")
            ) {
                orderItems.forEach { item ->
                    val product = ProductsTable
                        .selectAll()
                        .where { ProductsTable.id eq item.productId }
                        .singleOrNull()
                        ?: error("Product ${item.productId} not found")

                    val currentStock = product[ProductsTable.stockQuantity]
                    val restoredStock = currentStock.add(item.quantity)

                    ProductsTable.update(
                        where = { ProductsTable.id eq item.productId }
                    ) {
                        it[ProductsTable.stockQuantity] = restoredStock
                        it[ProductsTable.updatedAt] = System.currentTimeMillis()
                    }
                }
            }

            // ------------------------------------------
            // UPDATE ORDER STATUS
            // ------------------------------------------
            OrdersTable.update(
                where = { OrdersTable.id eq id }
            ) {
                it[OrdersTable.status] = status
                it[OrdersTable.updatedAt] = System.currentTimeMillis()
            }

            findById(id)
        }

    // ==========================================
    // ASSIGN DEALER
    // ==========================================

    fun assignDealer(
        orderId: Long,
        dealerId: Long?
    ): OrderRecord? =
        transaction {
            val order = OrdersTable
                .selectAll()
                .where { OrdersTable.id eq orderId }
                .singleOrNull()
                ?: return@transaction null

            if (dealerId != null) {
                UsersTable
                    .selectAll()
                    .where { UsersTable.id eq dealerId }
                    .singleOrNull()
                    ?: error("Dealer $dealerId not found")
            }

            OrdersTable.update(
                where = { OrdersTable.id eq orderId }
            ) {
                it[OrdersTable.assignedDealerId] = dealerId
                it[OrdersTable.updatedAt] = System.currentTimeMillis()
            }

            findById(orderId)
        }

    // ==========================================
    // FIND ORDER ITEMS
    // ==========================================

    private fun findItemsByOrderId(
        orderId: Long
    ): List<OrderItemRecord> {
        return OrderItemsTable
            .join(
                ProductsTable,
                JoinType.INNER,
                additionalConstraint = {
                    OrderItemsTable.productId eq ProductsTable.id
                }
            )
            .selectAll()
            .where { OrderItemsTable.orderId eq orderId }
            .map { row ->
                OrderItemRecord(
                    id = row[OrderItemsTable.id].value,
                    orderId = row[OrderItemsTable.orderId].value,
                    productId = row[OrderItemsTable.productId].value,
                    productName = row[ProductsTable.name],
                    quantity = row[OrderItemsTable.quantity],
                    unitPrice = row[OrderItemsTable.unitPrice],
                    subtotal = row[OrderItemsTable.subtotal],
                    createdAt = row[OrderItemsTable.createdAt]
                )
            }
    }

    // ==========================================
    // MAPPING
    // ==========================================

    private fun ResultRow.toOrderRecord(
        items: List<OrderItemRecord>
    ): OrderRecord {
        return OrderRecord(
            id = this[OrdersTable.id].value,
            customerId = this[OrdersTable.customerId].value,
            assignedDealerId = this[OrdersTable.assignedDealerId]?.value,
            telephone = this[OrdersTable.telephone],
            location = this[OrdersTable.location],
            neededDate = this[OrdersTable.neededDate],
            status = this[OrdersTable.status],
            notes = this[OrdersTable.notes],
            totalAmount = this[OrdersTable.totalAmount],
            createdAt = this[OrdersTable.createdAt],
            updatedAt = this[OrdersTable.updatedAt],
            orderType = this[OrdersTable.orderType],
            items = items
        )
    }
}

// ==========================================
// CREATE ORDER ITEM
// ==========================================

data class CreateOrderItemRecord(
    val productId: Long,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val subtotal: BigDecimal
)