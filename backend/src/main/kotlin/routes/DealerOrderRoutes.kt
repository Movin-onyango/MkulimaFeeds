package com.movofeeds.routes

import com.movofeeds.models.DealerOrderItemResponse
import com.movofeeds.models.DealerOrderResponse
import com.movofeeds.repository.OrderRecord
import com.movofeeds.security.Permission
import com.movofeeds.security.RbacGuard
import com.movofeeds.service.OrderService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import com.movofeeds.repository.UserRepository

fun Route.dealerOrderRoutes(
    orderService: OrderService
) {

    authenticate("auth-jwt") {

        // ==========================================================
        // DEALER — ALL ASSIGNED ORDERS
        // ==========================================================

        get("/api/dealer/orders") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.VIEW_ASSIGNED_ORDERS
                )
            } catch (e: IllegalAccessException) {
                return@get call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            val dealerId =
                principal!!
                    .payload
                    .getClaim("userId")
                    .asLong()

            if (dealerId == null || dealerId <= 0) {

                return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid dealer identity"
                    )
                )
            }

            try {

                val orders =
                    orderService.getDealerOrders(dealerId)

                call.respond(
                    orders.map { order ->
                        order.toDealerResponse()
                    }
                )

            } catch (e: Exception) {

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Failed to load dealer orders"
                                )
                    )
                )
            }
        }

        // ==========================================================
        // DEALER — SINGLE ORDER
        // ==========================================================

        get("/api/dealer/orders/{id}") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.VIEW_ASSIGNED_ORDERS
                )
            } catch (e: IllegalAccessException) {
                return@get call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            val dealerId =
                principal!!
                    .payload
                    .getClaim("userId")
                    .asLong()

            if (dealerId == null || dealerId <= 0) {

                return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid dealer identity"
                    )
                )
            }

            val orderId =
                call.parameters["id"]
                    ?.toLongOrNull()

            if (orderId == null || orderId <= 0) {

                return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid order ID"
                    )
                )
            }

            try {

                val order =
                    orderService.getDealerOrder(
                        dealerId = dealerId,
                        orderId = orderId
                    )

                call.respond(
                    order.toDealerResponse()
                )

            } catch (e: IllegalAccessException) {

                call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "You are not authorized to access this order"
                                )
                    )
                )

            } catch (e: NoSuchElementException) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Order not found"
                                )
                    )
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid order request"
                                )
                    )
                )

            } catch (e: Exception) {

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Failed to load order"
                                )
                    )
                )
            }
        }

        // ==========================================================
        // DEALER — UPDATE ORDER STATUS
        // ==========================================================

        put("/api/dealer/orders/{id}/status") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.UPDATE_ASSIGNED_ORDER_STATUS
                )
            } catch (e: IllegalAccessException) {
                return@put call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            val dealerId =
                principal!!
                    .payload
                    .getClaim("userId")
                    .asLong()

            if (dealerId == null || dealerId <= 0) {

                return@put call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid dealer identity"
                    )
                )
            }

            val orderId =
                call.parameters["id"]
                    ?.toLongOrNull()

            if (orderId == null || orderId <= 0) {

                return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid order ID"
                    )
                )
            }

            try {

                val body =
                    call.receive<Map<String, String>>()

                val requestedStatus =
                    body["status"]
                        ?.trim()
                        ?.uppercase()

                if (requestedStatus.isNullOrBlank()) {

                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "status" to "ERROR",
                            "message" to "Order status is required"
                        )
                    )
                }

                val updatedOrder =
                    orderService.updateDealerOrderStatus(
                        dealerId = dealerId,
                        orderId = orderId,
                        status = requestedStatus
                    )

                call.respond(
                    updatedOrder.toDealerResponse()
                )

            } catch (e: IllegalAccessException) {

                call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "You are not authorized to update this order"
                                )
                    )
                )

            } catch (e: NoSuchElementException) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Order not found"
                                )
                    )
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid order status"
                                )
                    )
                )

            } catch (e: Exception) {

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Failed to update order"
                                )
                    )
                )
            }
        }
    }
}

// ==========================================================
// ORDER RECORD → DEALER API RESPONSE
// ==========================================================

private fun OrderRecord.toDealerResponse(): DealerOrderResponse {

    val customer =
        UserRepository()
            .findById(customerId)

    val customerName =
        customer?.name
            ?: "Unknown Customer"

    return DealerOrderResponse(
        id = id,
        customerId = customerId,
        customerName = customerName,
        telephone = telephone,
        location = location,
        neededDate = neededDate,
        status = status,
        notes = notes,
        totalAmount = totalAmount.toPlainString(),
        assignedDealerId =
            assignedDealerId
                ?: error("Assigned dealer is missing"),
        createdAt = createdAt,
        updatedAt = updatedAt,
        items = items.map { item ->

            DealerOrderItemResponse(
                id = item.id,
                orderId = item.orderId,
                productId = item.productId,
                productName = item.productName,
                quantity = item.quantity.toPlainString(),
                unitPrice = item.unitPrice.toPlainString(),
                subtotal = item.subtotal.toPlainString(),
                createdAt = item.createdAt
            )
        }
    )
}