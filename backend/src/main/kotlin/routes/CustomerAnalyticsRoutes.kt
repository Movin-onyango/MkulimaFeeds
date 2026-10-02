package com.movofeeds.routes

import com.movofeeds.security.RoleAuthorization
import com.movofeeds.service.CustomerAnalyticsService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.customerAnalyticsRoutes(
    customerAnalyticsService: CustomerAnalyticsService
) {
    authenticate("auth-jwt") {
        get("/api/admin/customers/analytics") {

            val principal = call.principal<JWTPrincipal>()

            if (
                principal == null ||
                !RoleAuthorization.hasManagementRole(principal)
            ) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to
                                "You are not authorized to view customer analytics"
                    )
                )
                return@get
            }

            try {
                call.respond(
                    customerAnalyticsService.getAnalytics()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Failed to load customer analytics"
                                )
                    )
                )
            }
        }
    }
}