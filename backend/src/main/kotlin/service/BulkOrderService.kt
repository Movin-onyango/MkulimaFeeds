package com.movofeeds.service

import com.movofeeds.models.BulkOrderValidationResponse
import com.movofeeds.repository.OrderRecord
import com.movofeeds.repository.OrderRepository
import com.movofeeds.repository.ProductRepository
import java.math.BigDecimal
import java.math.RoundingMode

class BulkOrderService(
    private val orderRepository: OrderRepository = OrderRepository(),
    private val productRepository: ProductRepository = ProductRepository(),
    private val settingsService: SettingsService = SettingsService()
) {

    /**
     * Validate a bulk order against the current settings.
     */
    fun validate(
        items: List<OrderItemInput>,
        totalAmount: BigDecimal
    ): BulkOrderValidationResponse {

        val minValue = settingsService.getDecimal(
            "bulk_order.min_value",
            BigDecimal("25000")
        )
        val minQty = settingsService.getInt(
            "bulk_order.min_qty_per_item",
            100
        )
        val maxQty = settingsService.getInt(
            "bulk_order.max_qty_per_item",
            5000
        )
        val leadTime = settingsService.getInt(
            "bulk_order.lead_time_days",
            3
        )
        val paymentTerms = settingsService.getString(
            "bulk_order.payment_terms",
            "PREPAID"
        )

        val errors = mutableListOf<String>()

        // ---------------------------------------------
        // Minimum order value
        // ---------------------------------------------
        if (totalAmount < minValue) {
            errors.add(
                "Total order value must be at least " +
                        "KES ${minValue.setScale(0, RoundingMode.HALF_UP)}. " +
                        "Current: KES ${totalAmount.setScale(0, RoundingMode.HALF_UP)}"
            )
        }

        // ---------------------------------------------
        // Per-item min / max quantity
        // ---------------------------------------------
        items.forEach { item ->
            val qty = item.quantity.toIntOrNull() ?: 0
            if (qty < minQty) {
                errors.add(
                    "Each product must be at least $minQty units. " +
                            "Product ID ${item.productId} has $qty."
                )
            }
            if (qty > maxQty) {
                errors.add(
                    "Each product must be at most $maxQty units. " +
                            "Product ID ${item.productId} has $qty."
                )
            }
        }

        // ---------------------------------------------
        // Lead time
        // ---------------------------------------------
        // (Handled by the caller — they check neededDate)

        return BulkOrderValidationResponse(
            valid = errors.isEmpty(),
            errors = errors,
            minimumOrderValue = minValue.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            minimumQtyPerItem = minQty,
            maximumQtyPerItem = maxQty,
            leadTimeDays = leadTime,
            paymentTerms = paymentTerms
        )
    }

    /**
     * Create a bulk order for a dealer.
     * Validates against settings, then delegates to OrderService.
     */
    fun createBulkOrder(
        dealerId: Long,
        telephone: String,
        location: String,
        neededDate: Long,
        notes: String?,
        items: List<OrderItemInput>,
        orderService: OrderService
    ): Result<OrderRecord> {

        // ---------------------------------------------
        // Pre-check: lead time
        // ---------------------------------------------
        val leadTimeDays = settingsService.getInt(
            "bulk_order.lead_time_days",
            3
        )
        val now = System.currentTimeMillis()
        val minNeededDate = now + (leadTimeDays.toLong() * 24L * 60L * 60L * 1000L)

        if (neededDate < minNeededDate) {
            return Result.failure(
                IllegalArgumentException(
                    "Bulk orders require $leadTimeDays days lead time. " +
                            "Please choose a delivery date at least $leadTimeDays days from now."
                )
            )
        }

        // ---------------------------------------------
        // Calculate total from items
        // ---------------------------------------------
        var totalAmount = BigDecimal.ZERO
        items.forEach { item ->
            val product = productRepository.findById(item.productId)
                ?: return Result.failure(
                    NoSuchElementException(
                        "Product ${item.productId} not found"
                    )
                )
            val qty = item.quantity.toBigDecimalOrNull()
                ?: return Result.failure(
                    IllegalArgumentException("Invalid quantity")
                )
            totalAmount = totalAmount.add(
                product.price.multiply(qty)
            )
        }
        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP)

        // ---------------------------------------------
        // Validate against settings
        // ---------------------------------------------
        val validation = validate(items, totalAmount)
        if (!validation.valid) {
            return Result.failure(
                IllegalArgumentException(validation.errors.joinToString("\n"))
            )
        }

        // ---------------------------------------------
        // Delegate to the standard OrderService with orderType=BULK
        // ---------------------------------------------
        return try {
            val order = orderService.createOrder(
                customerId = dealerId,
                telephone = telephone,
                location = location,
                neededDate = neededDate,
                notes = notes,
                items = items,
                orderType = "BULK"
            )
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}