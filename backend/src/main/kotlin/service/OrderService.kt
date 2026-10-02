package com.movofeeds.service

import com.movofeeds.repository.CreateOrderItemRecord
import com.movofeeds.repository.OrderRecord
import com.movofeeds.repository.OrderRepository
import java.math.BigDecimal
import java.math.RoundingMode

class OrderService(
    private val orderRepository: OrderRepository = OrderRepository(),
    private val productRepository:
    com.movofeeds.repository.ProductRepository =
        com.movofeeds.repository.ProductRepository(),
    private val notificationService: NotificationService =
        NotificationService()
) {

    // ==========================================
    // CREATE ORDER
    // ==========================================

    fun createOrder(
        customerId: Long,
        telephone: String,
        location: String,
        neededDate: Long,
        notes: String?,
        items: List<OrderItemInput>,
        orderType: String = "RETAIL"
    ): OrderRecord {

        require(customerId > 0) {
            "Invalid customer ID"
        }

        val normalizedTelephone =
            telephone.trim()

        val normalizedLocation =
            location.trim()

        require(normalizedTelephone.isNotBlank()) {
            "Telephone is required"
        }

        require(normalizedLocation.isNotBlank()) {
            "Location is required"
        }

        require(neededDate > 0) {
            "Needed date is required"
        }

        require(items.isNotEmpty()) {
            "Order must contain at least one product"
        }

        val normalizedOrderType =
            orderType.trim().uppercase()

        require(
            normalizedOrderType in setOf("RETAIL", "WHOLESALE")
        ) {
            "Order type must be RETAIL or WHOLESALE"
        }

        val orderItems =
            mutableListOf<CreateOrderItemRecord>()

        var totalAmount =
            BigDecimal.ZERO

        items.forEach { item ->

            require(item.productId > 0) {
                "Invalid product ID"
            }

            val quantity =
                parseQuantity(item.quantity)

            require(
                quantity > BigDecimal.ZERO
            ) {
                "Quantity must be greater than zero"
            }

            val product =
                productRepository.findById(
                    item.productId
                )
                    ?: throw NoSuchElementException(
                        "Product ${item.productId} not found"
                    )

            require(product.isActive) {
                "Product ${product.name} is not available"
            }

            val subtotal =
                product.price
                    .multiply(quantity)
                    .setScale(
                        2,
                        RoundingMode.HALF_UP
                    )

            orderItems +=
                CreateOrderItemRecord(
                    productId = product.id,
                    quantity = quantity,
                    unitPrice = product.price,
                    subtotal = subtotal
                )

            totalAmount =
                totalAmount.add(subtotal)
        }

        totalAmount =
            totalAmount.setScale(
                2,
                RoundingMode.HALF_UP
            )

        return orderRepository.createOrder(
            customerId = customerId,
            telephone = normalizedTelephone,
            location = normalizedLocation,
            neededDate = neededDate,
            notes = notes?.trim(),
            items = orderItems,
            totalAmount = totalAmount,
            orderType = normalizedOrderType
        )
    }

    // ==========================================
    // GET ORDER
    // ==========================================

    fun getOrderById(
        id: Long
    ): OrderRecord {

        require(id > 0) {
            "Invalid order ID"
        }

        return orderRepository.findById(id)
            ?: throw NoSuchElementException(
                "Order not found"
            )
    }

    // ==========================================
    // CUSTOMER ORDERS
    // ==========================================

    fun getCustomerOrders(
        customerId: Long
    ): List<OrderRecord> {

        require(customerId > 0) {
            "Invalid customer ID"
        }

        return orderRepository.findByCustomerId(
            customerId
        )
    }

    // ==========================================
    // CANCEL CUSTOMER ORDER
    // ==========================================

    fun cancelOrder(
        id: Long,
        customerId: Long
    ): OrderRecord {

        require(id > 0) {
            "Invalid order ID"
        }

        require(customerId > 0) {
            "Invalid customer ID"
        }

        val order =
            orderRepository.findById(id)
                ?: throw NoSuchElementException(
                    "Order not found"
                )

        // Customer can only cancel their own order.
        if (order.customerId != customerId) {
            throw IllegalAccessException(
                "You are not authorized to cancel this order"
            )
        }

        // Customers can cancel pending or confirmed orders.
        require(
            order.status == "PENDING" ||
                    order.status == "CONFIRMED"
        ) {
            "Only pending or confirmed orders can be cancelled"
        }

        return orderRepository.updateStatus(
            id = id,
            status = "CANCELLED"
        )
            ?: throw NoSuchElementException(
                "Order not found"
            )
    }

    // ==========================================
    // ADMIN — ALL ORDERS
    // ==========================================

    fun getAllOrders(): List<OrderRecord> {
        return orderRepository.findAll()
    }

    // ==========================================
    // ADMIN — SINGLE ORDER
    // ==========================================

    fun getOrderByIdForAdmin(
        id: Long
    ): OrderRecord {
        return getOrderById(id)
    }

    // ==========================================
    // ADMIN / GENERAL UPDATE ORDER STATUS
    // ==========================================

    fun updateOrderStatus(
        id: Long,
        status: String
    ): OrderRecord {

        require(id > 0) {
            "Invalid order ID"
        }

        val order =
            orderRepository.findById(id)
                ?: throw NoSuchElementException(
                    "Order not found"
                )

        val normalizedStatus =
            status.trim().uppercase()

        val allowedStatuses =
            setOf(
                "PENDING",
                "CONFIRMED",
                "PROCESSING",
                "READY",
                "COMPLETED",
                "CANCELLED"
            )

        require(
            normalizedStatus in allowedStatuses
        ) {
            "Invalid order status"
        }

        val validTransition =
            when (order.status) {

                "PENDING" ->
                    normalizedStatus == "CONFIRMED" ||
                            normalizedStatus == "CANCELLED"

                "CONFIRMED" ->
                    normalizedStatus == "PROCESSING" ||
                            normalizedStatus == "CANCELLED"

                "PROCESSING" ->
                    normalizedStatus == "READY" ||
                            normalizedStatus == "CANCELLED"

                "READY" ->
                    normalizedStatus == "COMPLETED"

                "COMPLETED",
                "CANCELLED" ->
                    false

                else ->
                    false
            }

        require(validTransition) {
            "Invalid order status transition: " +
                    "${order.status} -> $normalizedStatus"
        }

        val updatedOrder =
            orderRepository.updateStatus(
                id = id,
                status = normalizedStatus
            )
                ?: throw NoSuchElementException(
                    "Order not found"
                )

        // Notify the customer.
        try {

            notificationService.createNotification(
                userId = updatedOrder.customerId,
                orderId = updatedOrder.id,
                type = "ORDER_STATUS_UPDATE",
                title = "Order Status Updated",
                message =
                    "Your order #${updatedOrder.id} " +
                            "is now ${updatedOrder.status}."
            )

        } catch (_: Exception) {

            // The order update should remain successful
            // even if notification creation fails.
        }

        // Notify assigned dealer, when applicable.
        if (updatedOrder.assignedDealerId != null) {

            try {

                notificationService.createNotification(
                    userId =
                        updatedOrder.assignedDealerId,
                    orderId = updatedOrder.id,
                    type = "ORDER_STATUS_UPDATE",
                    title = "Order Status Updated",
                    message =
                        "Order #${updatedOrder.id} " +
                                "is now ${updatedOrder.status}."
                )

            } catch (_: Exception) {

                // The order update should remain successful
                // even if notification creation fails.
            }
        }

        return updatedOrder
    }

    // ==========================================
    // ASSIGN DEALER
    // ==========================================

    fun assignDealer(
        orderId: Long,
        dealerId: Long?
    ): OrderRecord {

        val updatedOrder =
            orderRepository.assignDealer(
                orderId = orderId,
                dealerId = dealerId
            )
                ?: throw IllegalArgumentException(
                    "Order $orderId not found"
                )

        if (dealerId != null) {

            try {

                notificationService.createNotification(
                    userId = dealerId,
                    orderId = updatedOrder.id,
                    type = "ORDER_ASSIGNED",
                    title = "New Order Assigned",
                    message =
                        "You have been assigned order " +
                                "#${updatedOrder.id}."
                )

            } catch (_: Exception) {

                // Assignment remains successful even if
                // notification creation fails.
            }
        }

        return updatedOrder
    }

    // ==========================================
    // DEALER ORDERS
    // ==========================================

    fun getDealerOrders(
        dealerId: Long
    ): List<OrderRecord> {

        require(dealerId > 0) {
            "Invalid dealer ID"
        }

        return orderRepository.findByAssignedDealerId(
            dealerId
        )
    }

    // ==========================================
    // DEALER — GET SINGLE ORDER
    // ==========================================

    fun getDealerOrder(
        dealerId: Long,
        orderId: Long
    ): OrderRecord {

        require(dealerId > 0) {
            "Invalid dealer ID"
        }

        require(orderId > 0) {
            "Invalid order ID"
        }

        val order =
            orderRepository.findById(orderId)
                ?: throw NoSuchElementException(
                    "Order not found"
                )

        if (order.assignedDealerId != dealerId) {

            throw IllegalAccessException(
                "You are not authorized to access this order"
            )
        }

        return order
    }

    // ==========================================
    // DEALER — UPDATE ORDER STATUS
    // ==========================================

    fun updateDealerOrderStatus(
        dealerId: Long,
        orderId: Long,
        status: String
    ): OrderRecord {

        require(dealerId > 0) {
            "Invalid dealer ID"
        }

        require(orderId > 0) {
            "Invalid order ID"
        }

        val normalizedStatus =
            status.trim().uppercase()

        val allowedDealerStatuses =
            setOf(
                "CONFIRMED",
                "PROCESSING",
                "READY",
                "COMPLETED"
            )

        require(
            normalizedStatus in allowedDealerStatuses
        ) {
            "Dealers can only update orders to " +
                    "CONFIRMED, PROCESSING, READY, or COMPLETED"
        }

        val currentOrder =
            orderRepository.findById(orderId)
                ?: throw NoSuchElementException(
                    "Order not found"
                )

        // A dealer may only update orders assigned to them.
        if (
            currentOrder.assignedDealerId != dealerId
        ) {

            throw IllegalAccessException(
                "You are not authorized to update this order"
            )
        }

        val currentStatus =
            currentOrder.status
                .trim()
                .uppercase()

        val allowedTransition =
            when (currentStatus) {

                // Dealer accepts a newly assigned order.
                "PENDING" ->
                    normalizedStatus == "CONFIRMED"

                "CONFIRMED" ->
                    normalizedStatus == "PROCESSING"

                "PROCESSING" ->
                    normalizedStatus == "READY"

                "READY" ->
                    normalizedStatus == "COMPLETED"

                "COMPLETED",
                "CANCELLED" ->
                    false

                else -> false
            }

        require(allowedTransition) {
            "Invalid dealer status transition: " +
                    "$currentStatus -> $normalizedStatus"
        }

        val updatedOrder =
            orderRepository.updateStatus(
                id = orderId,
                status = normalizedStatus
            )
                ?: throw NoSuchElementException(
                    "Order not found"
                )

        // ==========================================
        // CUSTOMER STATUS NOTIFICATION
        // ==========================================

        try {

            notificationService.createNotification(
                userId = updatedOrder.customerId,
                orderId = updatedOrder.id,
                type = "ORDER_STATUS_UPDATE",
                title = "Order Status Updated",
                message =
                    "Your order #${updatedOrder.id} " +
                            "is now ${updatedOrder.status}."
            )

        } catch (_: Exception) {

            // The dealer's status update remains successful
            // even if notification creation fails.
        }

        return updatedOrder
    }

    // ==========================================
    // QUANTITY PARSER
    // ==========================================

    private fun parseQuantity(
        value: String
    ): BigDecimal {

        return try {

            BigDecimal(
                value.trim()
            ).setScale(
                2,
                RoundingMode.HALF_UP
            )

        } catch (_: NumberFormatException) {

            throw IllegalArgumentException(
                "Quantity must be a valid number"
            )
        }
    }
}

// ==========================================
// ORDER ITEM INPUT
// ==========================================

data class OrderItemInput(
    val productId: Long,
    val quantity: String
)