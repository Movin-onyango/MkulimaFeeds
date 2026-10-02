package com.movofeeds.routes

import com.movofeeds.models.MarkNotificationReadRequest
import com.movofeeds.models.NotificationResponse
import com.movofeeds.repository.NotificationRecord
import com.movofeeds.service.NotificationService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put

fun Route.notificationRoutes(
    notificationService: NotificationService
) {

    authenticate("auth-jwt") {

        get("/api/notifications") {

            try {

                val principal =
                    call.principal<JWTPrincipal>()
                        ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf(
                                "status" to "ERROR",
                                "message" to
                                        "Authentication required"
                            )
                        )

                val userId =
                    principal.payload
                        .getClaim("userId")
                        .asLong()
                        ?: throw IllegalArgumentException(
                            "Invalid user ID"
                        )

                val notifications =
                    notificationService
                        .getUserNotifications(
                            userId
                        )

                call.respond(
                    notifications.map {
                        it.toResponse()
                    }
                )

            } catch (
                e: IllegalArgumentException
            ) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid notification request"
                                )
                    )
                )
            }
        }

        put("/api/notifications/{id}/read") {

            try {

                val principal =
                    call.principal<JWTPrincipal>()
                        ?: return@put call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf(
                                "status" to "ERROR",
                                "message" to
                                        "Authentication required"
                            )
                        )

                val userId =
                    principal.payload
                        .getClaim("userId")
                        .asLong()
                        ?: throw IllegalArgumentException(
                            "Invalid user ID"
                        )

                val notificationId =
                    call.parameters["id"]
                        ?.toLongOrNull()
                        ?: throw IllegalArgumentException(
                            "Invalid notification ID"
                        )

                val request =
                    try {
                        call.receive<
                                MarkNotificationReadRequest
                                >()
                    } catch (_: Exception) {
                        MarkNotificationReadRequest()
                    }

                val notification =
                    if (request.isRead) {

                        notificationService.markAsRead(
                            userId = userId,
                            notificationId =
                                notificationId
                        )

                    } else {

                        throw IllegalArgumentException(
                            "Only marking notifications as read is currently supported"
                        )
                    }

                call.respond(
                    notification.toResponse()
                )

            } catch (
                e: NoSuchElementException
            ) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Notification not found"
                                )
                    )
                )

            } catch (
                e: IllegalArgumentException
            ) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid notification request"
                                )
                    )
                )
            }
        }

        put("/api/notifications/read-all") {

            try {

                val principal =
                    call.principal<JWTPrincipal>()
                        ?: return@put call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf(
                                "status" to "ERROR",
                                "message" to
                                        "Authentication required"
                            )
                        )

                val userId =
                    principal.payload
                        .getClaim("userId")
                        .asLong()
                        ?: throw IllegalArgumentException(
                            "Invalid user ID"
                        )

                notificationService
                    .markAllAsRead(userId)

                call.respond(
                    mapOf(
                        "status" to "OK",
                        "message" to
                                "All notifications marked as read"
                    )
                )

            } catch (
                e: IllegalArgumentException
            ) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid notification request"
                                )
                    )
                )
            }
        }
    }
}

private fun NotificationRecord.toResponse():
        NotificationResponse {

    return NotificationResponse(
        id = id,
        userId = userId,
        orderId = orderId,
        type = type,
        title = title,
        message = message,
        isRead = isRead,
        createdAt = createdAt
    )
}