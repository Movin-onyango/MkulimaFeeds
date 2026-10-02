package com.movofeeds.routes

import com.movofeeds.models.DealerCustomerResponse
import com.movofeeds.security.Permission
import com.movofeeds.security.RbacGuard
import com.movofeeds.service.DealerCustomerService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.dealerCustomerRoutes(
    dealerCustomerService: DealerCustomerService
) {

    authenticate("auth-jwt") {

        get("/api/dealer/customers") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.VIEW_ASSIGNED_CUSTOMERS
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

                val dealerId =
                    principal
                        ?.payload
                        ?.getClaim("userId")
                        ?.asLong()
                        ?: throw IllegalArgumentException(
                            "Invalid dealer ID"
                        )

                val customers =
                    dealerCustomerService
                        .getDealerCustomers(dealerId)

                call.respond(
                    customers.map { customer ->

                        DealerCustomerResponse(
                            id = customer.id,
                            name = customer.name,
                            phone = customer.phone,
                            email = customer.email,
                            totalOrders = customer.totalOrders,
                            activeOrders = customer.activeOrders,
                            completedOrders = customer.completedOrders
                        )
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
    }
}