package com.movofeeds.routes

import com.movofeeds.models.ChangeRoleRequest
import com.movofeeds.models.PromoteDealerRequest
import com.movofeeds.models.ReviewDealerApplicationRequest
import com.movofeeds.models.SuspendUserRequest
import com.movofeeds.security.Permission
import com.movofeeds.security.RbacGuard
import com.movofeeds.service.RoleService
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

fun Route.roleRoutes(
    roleService: RoleService = RoleService()
) {

    authenticate("auth-jwt") {

        // =========================================================
        // GET ROLE MATRIX (permissions reference)
        // =========================================================
        get("/api/admin/roles") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(principal, Permission.VIEW_ALL_USERS)
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
                call.respond(roleService.getRoleMatrix())
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to load role matrix")
                    )
                )
            }
        }

        // =========================================================
        // PROMOTE CUSTOMER → DEALER
        // =========================================================
        post("/api/admin/users/{id}/promote") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(principal, Permission.PROMOTE_DEALERS)
            } catch (e: IllegalAccessException) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            val adminId = principal!!.payload
                .getClaim("userId").asLong()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("status" to "ERROR", "message" to "Invalid token")
                )

            val userId = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "ERROR", "message" to "Invalid user ID")
                )

            try {
                val request = call.receive<PromoteDealerRequest>()

                roleService
                    .promoteToDealer(userId, adminId, request.reason)
                    .fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                mapOf(
                                    "status" to "OK",
                                    "message" to "User promoted to dealer"
                                )
                            )
                        },
                        onFailure = { e ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf(
                                    "status" to "ERROR",
                                    "message" to (e.message ?: "Promotion failed")
                                )
                            )
                        }
                    )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Promotion failed")
                    )
                )
            }
        }

        // =========================================================
        // DEMOTE DEALER → CUSTOMER
        // =========================================================
        post("/api/admin/users/{id}/demote") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(principal, Permission.DEMOTE_DEALERS)
            } catch (e: IllegalAccessException) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            val adminId = principal!!.payload
                .getClaim("userId").asLong()!!

            val userId = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "ERROR", "message" to "Invalid user ID")
                )

            try {
                val request = call.receive<PromoteDealerRequest>()

                roleService
                    .demoteToCustomer(userId, adminId, request.reason)
                    .fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                mapOf(
                                    "status" to "OK",
                                    "message" to "User demoted to customer"
                                )
                            )
                        },
                        onFailure = { e ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf(
                                    "status" to "ERROR",
                                    "message" to (e.message ?: "Demotion failed")
                                )
                            )
                        }
                    )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Demotion failed")
                    )
                )
            }
        }

        // =========================================================
        // SUSPEND USER
        // =========================================================
        post("/api/admin/users/{id}/suspend") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(principal, Permission.SUSPEND_USERS)
            } catch (e: IllegalAccessException) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            val adminId = principal!!.payload
                .getClaim("userId").asLong()!!

            val userId = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "ERROR", "message" to "Invalid user ID")
                )

            try {
                val request = call.receive<SuspendUserRequest>()

                if (request.reason.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "status" to "ERROR",
                            "message" to "Reason is required"
                        )
                    )
                }

                roleService
                    .suspendUser(userId, adminId, request.reason)
                    .fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                mapOf(
                                    "status" to "OK",
                                    "message" to "User suspended"
                                )
                            )
                        },
                        onFailure = { e ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf(
                                    "status" to "ERROR",
                                    "message" to (e.message ?: "Suspend failed")
                                )
                            )
                        }
                    )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Suspend failed")
                    )
                )
            }
        }

        // =========================================================
        // REACTIVATE USER
        // =========================================================
        post("/api/admin/users/{id}/reactivate") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(principal, Permission.SUSPEND_USERS)
            } catch (e: IllegalAccessException) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            val adminId = principal!!.payload
                .getClaim("userId").asLong()!!

            val userId = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "ERROR", "message" to "Invalid user ID")
                )

            try {
                val request = call.receive<ReviewDealerApplicationRequest>()

                roleService
                    .reactivateUser(userId, adminId, request.reason)
                    .fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                mapOf(
                                    "status" to "OK",
                                    "message" to "User reactivated"
                                )
                            )
                        },
                        onFailure = { e ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf(
                                    "status" to "ERROR",
                                    "message" to (e.message ?: "Reactivate failed")
                                )
                            )
                        }
                    )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Reactivate failed")
                    )
                )
            }
        }

        // =========================================================
        // DEALER APPLICATIONS — LIST
        // =========================================================
        get("/api/admin/dealer-applications") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(principal, Permission.PROMOTE_DEALERS)
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
                call.respond(roleService.getPendingDealerApplications())
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to load applications")
                    )
                )
            }
        }

        // =========================================================
        // DEALER APPLICATIONS — APPROVE
        // =========================================================
        post("/api/admin/dealer-applications/{id}/approve") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(principal, Permission.PROMOTE_DEALERS)
            } catch (e: IllegalAccessException) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            val adminId = principal!!.payload
                .getClaim("userId").asLong()!!

            val userId = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "ERROR", "message" to "Invalid user ID")
                )

            try {
                val request = call.receive<ReviewDealerApplicationRequest>()

                roleService
                    .approveDealerApplication(userId, adminId, request.reason)
                    .fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                mapOf(
                                    "status" to "OK",
                                    "message" to "Application approved"
                                )
                            )
                        },
                        onFailure = { e ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf(
                                    "status" to "ERROR",
                                    "message" to (e.message ?: "Approval failed")
                                )
                            )
                        }
                    )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Approval failed")
                    )
                )
            }
        }

        // =========================================================
        // DEALER APPLICATIONS — REJECT
        // =========================================================
        post("/api/admin/dealer-applications/{id}/reject") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(principal, Permission.PROMOTE_DEALERS)
            } catch (e: IllegalAccessException) {
                return@post call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            val adminId = principal!!.payload
                .getClaim("userId").asLong()!!

            val userId = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("status" to "ERROR", "message" to "Invalid user ID")
                )

            try {
                val request = call.receive<ReviewDealerApplicationRequest>()

                roleService
                    .rejectDealerApplication(userId, adminId, request.reason)
                    .fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                mapOf(
                                    "status" to "OK",
                                    "message" to "Application rejected"
                                )
                            )
                        },
                        onFailure = { e ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf(
                                    "status" to "ERROR",
                                    "message" to (e.message ?: "Rejection failed")
                                )
                            )
                        }
                    )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Rejection failed")
                    )
                )
            }
        }

        // =========================================================
        // AUDIT LOG
        // =========================================================
        get("/api/admin/audit-log") {

            val principal = call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(principal, Permission.VIEW_AUDIT_LOG)
            } catch (e: IllegalAccessException) {
                return@get call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Access denied")
                    )
                )
            }

            val limit = call.request.queryParameters["limit"]
                ?.toIntOrNull()
                ?.coerceIn(1, 500)
                ?: 100

            try {
                call.respond(roleService.getRecentAuditLog(limit))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to load audit log")
                    )
                )
            }
        }
    }
}