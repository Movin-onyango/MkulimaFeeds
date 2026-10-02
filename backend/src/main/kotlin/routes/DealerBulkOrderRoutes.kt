package com.movofeeds.routes

import com.movofeeds.models.BulkOrderRequest
import com.movofeeds.models.OrderItemResponse
import com.movofeeds.models.OrderResponse
import com.movofeeds.repository.UserRepository
import com.movofeeds.security.Permission
import com.movofeeds.security.RbacGuard
import com.movofeeds.service.BulkOrderService
import com.movofeeds.service.OrderItemInput
import com.movofeeds.service.OrderService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.dealerBulkOrderRoutes(
    bulkOrderService: BulkOrderService = BulkOrderService(),
    orderService: OrderService = OrderService()
) {

    authenticate("auth-jwt") {

        // =========================================================
        // VALIDATE (dry-run — checks rules without creating order)
        // =========================================================
        post("/api/dealer/orders/bulk/validate") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(principal, Permission.PLACE_BULK_ORDER)
            } catch (e: IllegalAccessException) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            try {
                val request = call.receive<BulkOrderRequest>()
                val items = request.items.map {
                    OrderItemInput(
                        productId = it.productId,
                        quantity = it.quantity
                    )
                }
                val validation = bulkOrderService.validate(
                    items = items,
                    totalAmount = java.math.BigDecimal.ZERO
                )
                call.respond(validation)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Validation failed")
                    )
                )
            }
        }

        // =========================================================
        // CREATE BULK ORDER
        // =========================================================
        post("/api/dealer/orders/bulk") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(principal, Permission.PLACE_BULK_ORDER)
            } catch (e: IllegalAccessException) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            val dealerId = principal!!.payload
                .getClaim("userId").asLong()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("status" to "ERROR", "message" to "Invalid token")
                )

            try {
                val request = call.receive<BulkOrderRequest>()

                if (request.items.isEmpty()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "status" to "ERROR",
                            "message" to "Order must contain at least one product"
                        )
                    )
                }

                val items = request.items.map {
                    OrderItemInput(
                        productId = it.productId,
                        quantity = it.quantity
                    )
                }

                bulkOrderService
                    .createBulkOrder(
                        dealerId = dealerId,
                        telephone = request.telephone,
                        location = request.location,
                        neededDate = request.neededDate,
                        notes = request.notes,
                        items = items,
                        orderService = orderService
                    )
                    .fold(
                        onSuccess = { order ->
                            call.respond(
                                HttpStatusCode.Created,
                                order.toBulkResponse()
                            )
                        },
                        onFailure = { e ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf(
                                    "status" to "ERROR",
                                    "message" to (e.message ?: "Failed to create bulk order")
                                )
                            )
                        }
                    )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to create bulk order")
                    )
                )
            }
        }

        // =========================================================
        // LIST OWN BULK ORDERS
        // =========================================================
        get("/api/dealer/orders/bulk") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(principal, Permission.PLACE_BULK_ORDER)
            } catch (e: IllegalAccessException) {
                return@get call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            val dealerId = principal!!.payload
                .getClaim("userId").asLong()
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("status" to "ERROR", "message" to "Invalid token")
                )

            try {
                val allOrders = orderService.getCustomerOrders(dealerId)
                val bulkOrders = allOrders.filter {
                    it.orderType.equals("BULK", ignoreCase = true)
                }
                call.respond(bulkOrders.map { it.toBulkResponse() })
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to load bulk orders")
                    )
                )
            }
        }
    }
}

private fun com.movofeeds.repository.OrderRecord.toBulkResponse(): OrderResponse {
    val customer = UserRepository().findById(customerId)
    val customerName = customer?.name ?: "Unknown"

    return OrderResponse(
        id = id,
        customerId = customerId,
        customerName = customerName,
        assignedDealerId = assignedDealerId,
        telephone = telephone,
        location = location,
        neededDate = neededDate,
        status = status,
        notes = notes,
        totalAmount = totalAmount.toPlainString(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        orderType = orderType,
        items = items.map {
            OrderItemResponse(
                id = it.id,
                orderId = it.orderId,
                productId = it.productId,
                productName = it.productName,
                quantity = it.quantity.toPlainString(),
                unitPrice = it.unitPrice.toPlainString(),
                subtotal = it.subtotal.toPlainString(),
                createdAt = it.createdAt
            )
        }
    )
}