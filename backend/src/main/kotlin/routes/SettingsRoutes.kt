package com.movofeeds.routes

import com.movofeeds.models.UpdateSettingsRequest
import com.movofeeds.models.UpdateSettingsResponse
import com.movofeeds.security.Permission
import com.movofeeds.security.RbacGuard
import com.movofeeds.service.SettingsService
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

fun Route.settingsRoutes(
    settingsService: SettingsService = SettingsService()
) {

    // =========================================================
    // PUBLIC BRANDING SETTINGS (no auth required)
    // =========================================================
    get("/api/public/branding") {
        try {
            val settings = settingsService
                .getSettingsByCategory("BRANDING")
                .associate { it.key to it.value }

            call.respond(settings)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "ERROR",
                    "message" to (e.message ?: "Failed to load branding")
                )
            )
        }
    }

    authenticate("auth-jwt") {

        // ---------------------------------------------
        // GET ALL SETTINGS
        // ---------------------------------------------
        get("/api/admin/settings") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.MANAGE_SETTINGS
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
                val settings = settingsService.getAllSettings()
                call.respond(settings)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to load settings")
                    )
                )
            }
        }

        // ---------------------------------------------
        // GET SETTINGS BY CATEGORY
        // ---------------------------------------------
        get("/api/admin/settings/{category}") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.MANAGE_SETTINGS
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

            val category = call.parameters["category"]
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Category is required"
                    )
                )

            try {
                val settings = settingsService.getSettingsByCategory(category)
                call.respond(settings)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to load settings")
                    )
                )
            }
        }

        // ---------------------------------------------
        // UPDATE SETTINGS (BULK)
        // ---------------------------------------------
        put("/api/admin/settings") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.MANAGE_SETTINGS
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

            val adminId = principal
                ?.payload
                ?.getClaim("userId")
                ?.asLong()
                ?: return@put call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid authentication token"
                    )
                )

            try {
                val request = call.receive<UpdateSettingsRequest>()

                val result = settingsService.updateSettings(
                    changes = request.changes,
                    adminId = adminId
                )

                result.fold(
                    onSuccess = { updated ->
                        call.respond(
                            HttpStatusCode.OK,
                            UpdateSettingsResponse(
                                status = "OK",
                                message = "Settings updated",
                                updated = updated
                            )
                        )
                    },
                    onFailure = { e ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "status" to "ERROR",
                                "message" to (
                                        e.message
                                            ?: "Failed to update settings"
                                        )
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to update settings")
                    )
                )
            }
        }

        // ---------------------------------------------
        // RESET TO DEFAULTS (ADMIN ONLY)
        // ---------------------------------------------
        post("/api/admin/settings/reset") {

            val principal = call.principal<JWTPrincipal>()

            if (
                principal == null ||
                !com.movofeeds.security.RoleAuthorization.hasManagementRole(principal)
            ) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Admin access required"
                    )
                )
            }

            val adminId = principal.payload
                .getClaim("userId")
                .asLong()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid authentication token"
                    )
                )

            try {
                settingsService
                    .resetToDefaults(adminId)
                    .fold(
                        onSuccess = { updated ->
                            call.respond(
                                HttpStatusCode.OK,
                                mapOf(
                                    "status" to "OK",
                                    "message" to "Settings reset to factory defaults",
                                    "settings" to updated
                                )
                            )
                        },
                        onFailure = { e ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf(
                                    "status" to "ERROR",
                                    "message" to (
                                            e.message
                                                ?: "Failed to reset settings"
                                            )
                                )
                            )
                        }
                    )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to reset settings")
                    )
                )
            }
        }
    }
}