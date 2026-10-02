package com.movofeeds.routes

import com.movofeeds.repository.UserRepository
import com.movofeeds.models.CreateOrderItemRequest
import com.movofeeds.models.CreateOrderRequest
import com.movofeeds.models.OrderItemResponse
import com.movofeeds.models.OrderResponse
import com.movofeeds.repository.OrderRecord
import com.movofeeds.service.OrderItemInput
import com.movofeeds.service.OrderService
import com.movofeeds.security.Permission
import com.movofeeds.security.RbacGuard
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import com.movofeeds.models.AssignDealerRequest

fun Route.orderRoutes(
    orderService: OrderService
) {

    authenticate("auth-jwt") {

        // ==========================================
        // CREATE ORDER
        // ==========================================

        post("/api/orders") {

            try {

                val principal =
                    call.principal<JWTPrincipal>()
                        ?: return@post call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf(
                                "status" to "ERROR",
                                "message" to "Authentication required"
                            )
                        )

                val customerId =
                    principal.payload
                        .getClaim("userId")
                        .asLong()
                        ?: throw IllegalArgumentException(
                            "Invalid user ID"
                        )

                val request =
                    call.receive<CreateOrderRequest>()

                val items =
                    request.items.map {
                        OrderItemInput(
                            productId = it.productId,
                            quantity = it.quantity
                        )
                    }

                val order =
                    orderService.createOrder(
                        customerId = customerId,
                        telephone = request.telephone,
                        location = request.location,
                        neededDate = request.neededDate,
                        notes = request.notes,
                        items = items
                    )

                call.respond(
                    HttpStatusCode.Created,
                    order.toResponse()
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid order data"
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
                                    ?: "Product not found"
                                )
                    )
                )
            }
        }

        // ==========================================
        // CUSTOMER ORDERS
        // ==========================================

        get("/api/orders") {

            try {

                val principal =
                    call.principal<JWTPrincipal>()
                        ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf(
                                "status" to "ERROR",
                                "message" to "Authentication required"
                            )
                        )

                val customerId =
                    principal.payload
                        .getClaim("userId")
                        .asLong()
                        ?: throw IllegalArgumentException(
                            "Invalid user ID"
                        )

                val orders =
                    orderService.getCustomerOrders(
                        customerId
                    )

                call.respond(
                    orders.map {
                        it.toResponse()
                    }
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid request"
                                )
                    )
                )
            }
        }

        // ==========================================
        // GET CUSTOMER ORDER
        // ==========================================

        get("/api/orders/{id}") {

            try {

                val principal =
                    call.principal<JWTPrincipal>()
                        ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf(
                                "status" to "ERROR",
                                "message" to "Authentication required"
                            )
                        )

                val customerId =
                    principal.payload
                        .getClaim("userId")
                        .asLong()
                        ?: throw IllegalArgumentException(
                            "Invalid user ID"
                        )

                val id =
                    call.parameters["id"]
                        ?.toLongOrNull()
                        ?: throw IllegalArgumentException(
                            "Invalid order ID"
                        )

                val order =
                    orderService.getOrderById(id)

                // Customer can only access their own order.
                if (order.customerId != customerId) {

                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf(
                            "status" to "ERROR",
                            "message" to "You are not authorized to view this order"
                        )
                    )
                }

                call.respond(
                    order.toResponse()
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid order ID"
                                )
                    )
                )

            } catch (e: NoSuchElementException) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Order not found"
                    )
                )
            }
        }

        // ==========================================
        // CUSTOMER — CANCEL ORDER
        // ==========================================

        put("/api/orders/{id}/cancel") {

            try {

                val principal =
                    call.principal<JWTPrincipal>()
                        ?: return@put call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf(
                                "status" to "ERROR",
                                "message" to "Authentication required"
                            )
                        )

                val customerId =
                    principal.payload
                        .getClaim("userId")
                        .asLong()
                        ?: throw IllegalArgumentException(
                            "Invalid user ID"
                        )

                val id =
                    call.parameters["id"]
                        ?.toLongOrNull()
                        ?: throw IllegalArgumentException(
                            "Invalid order ID"
                        )

                val order =
                    orderService.cancelOrder(
                        id = id,
                        customerId = customerId
                    )

                call.respond(
                    order.toResponse()
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid request"
                                )
                    )
                )

            } catch (e: IllegalAccessException) {

                call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "You are not authorized to cancel this order"
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
            }
        }

        // ==========================================
        // ADMIN — ALL ORDERS
        // ==========================================

        get("/api/admin/orders") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.VIEW_ALL_ORDERS
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

            call.respond(
                orderService
                    .getAllOrders()
                    .map {
                        it.toResponse()
                    }
            )
        }

        // ==========================================
        // ADMIN — SINGLE ORDER
        // ==========================================

        get("/api/admin/orders/{id}") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.VIEW_ALL_ORDERS
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

            try {

                val id =
                    call.parameters["id"]
                        ?.toLongOrNull()
                        ?: throw IllegalArgumentException(
                            "Invalid order ID"
                        )

                val order =
                    orderService.getOrderByIdForAdmin(id)

                call.respond(
                    order.toResponse()
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid order ID"
                                )
                    )
                )

            } catch (e: NoSuchElementException) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Order not found"
                    )
                )
            }
        }

        // ==========================================
        // ADMIN — UPDATE STATUS
        // ==========================================

        put("/api/admin/orders/{id}/status") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.UPDATE_ANY_ORDER_STATUS
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

            try {

                val id =
                    call.parameters["id"]
                        ?.toLongOrNull()
                        ?: throw IllegalArgumentException(
                            "Invalid order ID"
                        )

                val body =
                    call.receive<Map<String, String>>()

                val status =
                    body["status"]
                        ?: throw IllegalArgumentException(
                            "Order status is required"
                        )

                val order =
                    orderService.updateOrderStatus(
                        id = id,
                        status = status
                    )

                call.respond(
                    order.toResponse()
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

            } catch (e: NoSuchElementException) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Order not found"
                    )
                )
            }
        }

        // ==========================================
        // ADMIN — ASSIGN DEALER (legacy alias)
        // ==========================================

        put("/api/admin/orders/{id}/dealer") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.ASSIGN_DEALERS_TO_ORDERS
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

            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid order ID")
                )

            val request = call.receive<Map<String, Long?>>()
            val dealerId = request["dealerId"]

            try {
                val order = orderService.assignDealer(
                    orderId = id,
                    dealerId = dealerId
                )
                call.respond(
                    HttpStatusCode.OK,
                    order.toResponse()
                )
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "message" to (e.message ?: "Order not found")
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "message" to (e.message ?: "Failed to assign dealer")
                    )
                )
            }
        }

        // ==========================================
        // ADMIN — ASSIGN DEALER (canonical endpoint)
        // ==========================================

        put("/api/admin/orders/{id}/assign-dealer") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.ASSIGN_DEALERS_TO_ORDERS
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

            try {

                val orderId =
                    call.parameters["id"]
                        ?.toLongOrNull()
                        ?: throw IllegalArgumentException(
                            "Invalid order ID"
                        )

                val request =
                    call.receive<AssignDealerRequest>()

                val updatedOrder =
                    orderService.assignDealer(
                        orderId = orderId,
                        dealerId = request.dealerId
                    )

                call.respond(
                    updatedOrder.toResponse()
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid dealer assignment"
                                )
                    )
                )

            } catch (e: NoSuchElementException) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "status" to "ERROR",
                        "message" to
                                "Order not found"
                    )
                )
            }
        }
    }
}

// ==========================================
// ORDER → API RESPONSE
// ==========================================

private fun OrderRecord.toResponse(): OrderResponse {

    val customer =
        UserRepository()
            .findById(customerId)

    val customerName =
        customer?.name
            ?: "Unknown Customer"

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
        orderType = "RETAIL",
        updatedAt = updatedAt,
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