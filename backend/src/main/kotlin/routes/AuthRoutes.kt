package com.movofeeds.routes

import com.movofeeds.models.AuthMeResponse
import com.movofeeds.models.AuthResponse
import com.movofeeds.models.ChangePasswordRequest
import com.movofeeds.models.ForgotPasswordInitiateRequest
import com.movofeeds.models.ForgotPasswordInitiateResponse
import com.movofeeds.models.ForgotPasswordResetRequest
import com.movofeeds.models.ForgotPasswordVerifyRequest
import com.movofeeds.models.LoginRequest
import com.movofeeds.models.RegisterInitiateRequest
import com.movofeeds.models.RegisterInitiateResponse
import com.movofeeds.models.RegisterRequest
import com.movofeeds.models.RegisterVerifyRequest
import com.movofeeds.models.UnifiedLoginRequest
import com.movofeeds.models.UserResponse
import com.movofeeds.security.JwtService
import com.movofeeds.service.AuthService
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

fun Route.authRoutes(
    authService: AuthService
) {

    // =========================================================
    // LEGACY REGISTER (kept for compatibility)
    // =========================================================
    post("/api/auth/register") {
        try {
            val request = call.receive<RegisterRequest>()
            val user = authService.register(
                name = request.name,
                phone = request.phone,
                email = request.email,
                password = request.password
            )
            val token = JwtService.generateToken(user)
            call.respond(
                HttpStatusCode.Created,
                AuthResponse(
                    token = token,
                    user = user.toUserResponse()
                )
            )
        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "ERROR",
                    "message" to (e.message ?: "Invalid registration data")
                )
            )
        }
    }

    // =========================================================
    // REGISTER — STEP 1: INITIATE
    // =========================================================
    post("/api/auth/register/initiate") {
        try {
            val request = call.receive<RegisterInitiateRequest>()

            val result = authService.initiateRegistration(
                name = request.name,
                email = request.email,
                phone = request.phone,
                password = request.password
            )

            result.fold(
                onSuccess = { (channel, destination) ->
                    call.respond(
                        HttpStatusCode.OK,
                        RegisterInitiateResponse(
                            status = "OK",
                            message = "Verification code sent to your ${channel.lowercase()}.",
                            channel = channel,
                            destination = destination,
                            expiresInSeconds = 600
                        )
                    )
                },
                onFailure = { e ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "status" to "ERROR",
                            "message" to (e.message ?: "Registration failed")
                        )
                    )
                }
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "ERROR",
                    "message" to (e.message ?: "Registration failed")
                )
            )
        }
    }

    // =========================================================
    // REGISTER — STEP 2: VERIFY
    // =========================================================
    post("/api/auth/register/verify") {
        try {
            val request = call.receive<RegisterVerifyRequest>()

            val result = authService.verifyRegistration(
                destination = request.destination,
                code = request.code
            )

            result.fold(
                onSuccess = { user ->
                    val token = JwtService.generateToken(user)
                    call.respond(
                        HttpStatusCode.Created,
                        AuthResponse(
                            token = token,
                            user = user.toUserResponse()
                        )
                    )
                },
                onFailure = { e ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "status" to "ERROR",
                            "message" to (e.message ?: "Verification failed")
                        )
                    )
                }
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "ERROR",
                    "message" to (e.message ?: "Verification failed")
                )
            )
        }
    }

    // =========================================================
    // UNIFIED LOGIN (email OR phone)
    // =========================================================
    post("/api/auth/login") {
        try {
            // Accept both old (email) and new (identifier) payloads.
            // Try the new one first; if that fails, fall back to legacy.
            val rawBody = call.receive<UnifiedLoginRequest>()

            val user = authService.loginWithIdentifier(
                identifier = rawBody.identifier,
                password = rawBody.password
            )

            val token = JwtService.generateToken(user)

            call.respond(
                HttpStatusCode.OK,
                AuthResponse(
                    token = token,
                    user = user.toUserResponse()
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "status" to "ERROR",
                    "message" to (e.message ?: "Invalid login credentials")
                )
            )
        }
    }

    // =========================================================
    // FORGOT PASSWORD — STEP 1: INITIATE
    // =========================================================
    post("/api/auth/forgot-password/initiate") {
        try {
            val request = call.receive<ForgotPasswordInitiateRequest>()

            val result = authService.initiatePasswordReset(
                identifier = request.identifier
            )

            result.fold(
                onSuccess = { payload ->
                    call.respond(
                        HttpStatusCode.OK,
                        ForgotPasswordInitiateResponse(
                            status = "OK",
                            message = "If the account exists, verification codes have been sent.",
                            emailDestination = payload.emailDestination,
                            phoneDestination = payload.phoneDestination
                        )
                    )
                },
                onFailure = { e ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "status" to "ERROR",
                            "message" to (e.message ?: "Request failed")
                        )
                    )
                }
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "ERROR",
                    "message" to (e.message ?: "Request failed")
                )
            )
        }
    }

    // =========================================================
    // FORGOT PASSWORD — STEP 2: VERIFY
    // =========================================================
    post("/api/auth/forgot-password/verify") {
        try {
            val request = call.receive<ForgotPasswordVerifyRequest>()

            val result = authService.verifyPasswordResetCodes(
                identifier = request.identifier,
                emailCode = request.emailCode,
                phoneCode = request.phoneCode
            )

            result.fold(
                onSuccess = { resetToken ->
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "status" to "OK",
                            "message" to "Codes verified. You may now reset your password.",
                            "resetToken" to resetToken
                        )
                    )
                },
                onFailure = { e ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "status" to "ERROR",
                            "message" to (e.message ?: "Verification failed")
                        )
                    )
                }
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "ERROR",
                    "message" to (e.message ?: "Verification failed")
                )
            )
        }
    }

    // =========================================================
    // FORGOT PASSWORD — STEP 3: RESET
    // =========================================================
    post("/api/auth/forgot-password/reset") {
        try {
            val request = call.receive<ForgotPasswordResetRequest>()

            val result = authService.resetPasswordWithToken(
                identifier = request.identifier,
                resetToken = request.resetToken,
                newPassword = request.newPassword
            )

            result.fold(
                onSuccess = {
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "status" to "OK",
                            "message" to "Password has been reset successfully."
                        )
                    )
                },
                onFailure = { e ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "status" to "ERROR",
                            "message" to (e.message ?: "Reset failed")
                        )
                    )
                }
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "ERROR",
                    "message" to (e.message ?: "Reset failed")
                )
            )
        }
    }

    // =========================================================
    // AUTHENTICATED ROUTES
    // =========================================================
    authenticate("auth-jwt") {

        get("/api/auth/me") {
            val principal = call.principal<JWTPrincipal>()
                ?: throw IllegalArgumentException("Authentication required")

            val userId = principal.payload.getClaim("userId").asLong()
                ?: throw IllegalArgumentException("Invalid user ID")
            val email = principal.payload.getClaim("email").asString()
                ?: throw IllegalArgumentException("Invalid email")
            val role = principal.payload.getClaim("role").asString()
                ?: throw IllegalArgumentException("Invalid role")

            call.respond(
                AuthMeResponse(
                    status = "OK",
                    userId = userId,
                    email = email,
                    role = role
                )
            )
        }

        put("/api/auth/change-password") {
            try {
                val principal = call.principal<JWTPrincipal>()
                    ?: throw IllegalArgumentException("Authentication required")

                val userId = principal.payload.getClaim("userId").asLong()
                    ?: throw IllegalArgumentException("Invalid user ID")

                val request = call.receive<ChangePasswordRequest>()

                authService.changePassword(
                    userId = userId,
                    currentPassword = request.currentPassword,
                    newPassword = request.newPassword
                )

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "status" to "OK",
                        "message" to "Password changed successfully"
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Unable to change password")
                    )
                )
            }
        }
    }
}

// =========================================================
// MAPPING
// =========================================================

private fun com.movofeeds.repository.UserRecord.toUserResponse() =
    UserResponse(
        id = id,
        name = name,
        phone = phone,
        email = email,
        role = role,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )