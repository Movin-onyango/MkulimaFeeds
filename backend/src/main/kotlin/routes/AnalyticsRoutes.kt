package com.movofeeds.routes

import com.movofeeds.service.AnalyticsService
import com.movofeeds.security.Permission
import com.movofeeds.security.RbacGuard
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.analyticsRoutes(
    analyticsService: AnalyticsService
) {

    authenticate("auth-jwt") {

        get("/api/admin/analytics") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.VIEW_ANALYTICS
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

                call.respond(
                    analyticsService.getOverview()
                )

            } catch (e: Exception) {

                e.printStackTrace()

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to
                                (
                                        e.message
                                            ?: "Failed to load analytics"
                                        )
                    )
                )
            }
        }
    }
}