package com.movofeeds.routes

import com.movofeeds.models.AdminUserResponse
import com.movofeeds.models.CreateAdminUserRequest
import com.movofeeds.models.MeResponse
import com.movofeeds.models.UpdateAdminUserRequest
import com.movofeeds.models.UserResponse
import com.movofeeds.repository.UserRepository
import com.movofeeds.security.Permission
import com.movofeeds.security.RbacGuard
import com.movofeeds.service.AdminUserService
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
import com.movofeeds.models.CreateSavedLocationRequest
import com.movofeeds.models.DeleteAccountRequest
import com.movofeeds.models.SavedLocationResponse
import com.movofeeds.models.UpdateProfileRequest
import com.movofeeds.models.UpdateSavedLocationRequest
import com.movofeeds.repository.SavedLocationRecord
import com.movofeeds.repository.SavedLocationRepository
import com.movofeeds.service.UserProfileService
import io.ktor.server.routing.delete

fun Route.userRoutes(
    userRepository: UserRepository = UserRepository(),
    adminUserService: AdminUserService = AdminUserService(
        userRepository = userRepository
    )
) {

    authenticate("auth-jwt") {

        // =====================================================
        // CURRENT AUTHENTICATED USER
        // =====================================================

        get("/api/users/me") {

            val principal =
                call.principal<JWTPrincipal>()

            val userId =
                principal
                    ?.payload
                    ?.getClaim("userId")
                    ?.asLong()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf(
                            "status" to "ERROR",
                            "message" to "Invalid authentication token"
                        )
                    )

            val user =
                userRepository.findById(userId)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf(
                            "status" to "ERROR",
                            "message" to "User not found"
                        )
                    )

            val response =
                MeResponse(
                    status = "OK",
                    user =
                        UserResponse(
                            id = user.id,
                            name = user.name,
                            phone = user.phone,
                            email = user.email,
                            role = user.role,
                            isActive = user.isActive,
                            createdAt = user.createdAt,
                            updatedAt = user.updatedAt
                        )
                )

            call.respond(
                HttpStatusCode.OK,
                response
            )
        }

        // =====================================================
        // PROFILE SUMMARY
        // =====================================================

        get("/api/users/me/profile") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asLong()
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid authentication token"
                    )
                )

            try {
                val service = UserProfileService()
                val profile = service.getProfile(userId)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf(
                            "status" to "ERROR",
                            "message" to "User not found"
                        )
                    )

                call.respond(profile)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to load profile")
                    )
                )
            }
        }

        // =====================================================
        // UPDATE OWN PROFILE
        // =====================================================

        put("/api/users/me") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asLong()
                ?: return@put call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid authentication token"
                    )
                )

            try {
                val request = call.receive<UpdateProfileRequest>()
                val service = UserProfileService()

                val result = service.updateOwnProfile(
                    userId = userId,
                    name = request.name,
                    email = request.email,
                    phone = request.phone
                )

                result.fold(
                    onSuccess = { updated ->
                        call.respond(
                            HttpStatusCode.OK,
                            mapOf(
                                "status" to "OK",
                                "message" to "Profile updated",
                                "user" to mapOf(
                                    "id" to updated.id,
                                    "name" to updated.name,
                                    "email" to updated.email,
                                    "phone" to updated.phone,
                                    "role" to updated.role
                                )
                            )
                        )
                    },
                    onFailure = { e ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "status" to "ERROR",
                                "message" to (e.message ?: "Update failed")
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Update failed")
                    )
                )
            }
        }

        // =====================================================
        // DELETE OWN ACCOUNT (SOFT)
        // =====================================================

        post("/api/users/me/delete") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asLong()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid authentication token"
                    )
                )

            try {
                val request = call.receive<DeleteAccountRequest>()
                val service = UserProfileService()

                val result = service.deleteOwnAccount(
                    userId = userId,
                    password = request.password,
                    confirmation = request.confirmation
                )

                result.fold(
                    onSuccess = {
                        call.respond(
                            HttpStatusCode.OK,
                            mapOf(
                                "status" to "OK",
                                "message" to "Account deleted"
                            )
                        )
                    },
                    onFailure = { e ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "status" to "ERROR",
                                "message" to (e.message ?: "Delete failed")
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Delete failed")
                    )
                )
            }
        }

        // =====================================================
        // SAVED LOCATIONS — LIST
        // =====================================================

        get("/api/users/me/locations") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asLong()
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid authentication token"
                    )
                )

            try {
                val repository = SavedLocationRepository()
                val locations = repository.findByUser(userId)
                call.respond(locations.map { it.toResponse() })
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to load locations")
                    )
                )
            }
        }

        // =====================================================
        // SAVED LOCATIONS — CREATE
        // =====================================================

        post("/api/users/me/locations") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asLong()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid authentication token"
                    )
                )

            try {
                val request = call.receive<CreateSavedLocationRequest>()

                if (request.label.isBlank() || request.address.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "status" to "ERROR",
                            "message" to "Label and address are required"
                        )
                    )
                }

                val repository = SavedLocationRepository()
                val created = repository.create(
                    userId = userId,
                    label = request.label.trim(),
                    address = request.address.trim(),
                    isDefault = request.isDefault
                )

                call.respond(
                    HttpStatusCode.Created,
                    created.toResponse()
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to create location")
                    )
                )
            }
        }

        // =====================================================
        // SAVED LOCATIONS — UPDATE
        // =====================================================

        put("/api/users/me/locations/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asLong()
                ?: return@put call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid authentication token"
                    )
                )

            try {
                val locationId = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "status" to "ERROR",
                            "message" to "Invalid location ID"
                        )
                    )

                val request = call.receive<UpdateSavedLocationRequest>()

                if (request.label.isBlank() || request.address.isBlank()) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "status" to "ERROR",
                            "message" to "Label and address are required"
                        )
                    )
                }

                val repository = SavedLocationRepository()
                val updated = repository.update(
                    id = locationId,
                    userId = userId,
                    label = request.label.trim(),
                    address = request.address.trim(),
                    isDefault = request.isDefault
                )

                if (updated == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf(
                            "status" to "ERROR",
                            "message" to "Location not found"
                        )
                    )
                } else {
                    call.respond(updated.toResponse())
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to update location")
                    )
                )
            }
        }

        // =====================================================
        // SAVED LOCATIONS — DELETE
        // =====================================================

        delete("/api/users/me/locations/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asLong()
                ?: return@delete call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "Invalid authentication token"
                    )
                )

            try {
                val locationId = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "status" to "ERROR",
                            "message" to "Invalid location ID"
                        )
                    )

                val repository = SavedLocationRepository()
                val deleted = repository.delete(locationId, userId)

                if (deleted) {
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "status" to "OK",
                            "message" to "Location deleted"
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf(
                            "status" to "ERROR",
                            "message" to "Location not found"
                        )
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (e.message ?: "Failed to delete location")
                    )
                )
            }
        }

        // =====================================================
        // ACTIVE DEALERS
        // =====================================================

        get("/api/admin/dealers") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.VIEW_ALL_USERS
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

            val dealers =
                userRepository.findActiveDealers()

            val response =
                dealers.map { dealer ->
                    UserResponse(
                        id = dealer.id,
                        name = dealer.name,
                        phone = dealer.phone,
                        email = dealer.email,
                        role = dealer.role,
                        isActive = dealer.isActive,
                        createdAt = dealer.createdAt,
                        updatedAt = dealer.updatedAt
                    )
                }

            call.respond(
                HttpStatusCode.OK,
                response
            )
        }

        // =====================================================
        // ADMIN — ALL USERS
        // =====================================================

        get("/api/admin/users") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.VIEW_ALL_USERS
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
                    adminUserService.getAllUsers()
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Failed to load users"
                                )
                    )
                )
            }
        }

        // =====================================================
        // ADMIN — SINGLE USER
        // =====================================================

        get("/api/admin/users/{id}") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.VIEW_ALL_USERS
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
                            "Invalid user ID"
                        )

                call.respond(
                    adminUserService.getUser(id)
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid user ID"
                                )
                    )
                )

            } catch (e: NoSuchElementException) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "User not found"
                    )
                )
            }
        }

        // =====================================================
        // ADMIN — CREATE USER
        // =====================================================

        post("/api/admin/users") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.CREATE_STAFF
                )
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

                val request =
                    call.receive<CreateAdminUserRequest>()

                val user =
                    adminUserService.createUser(request)

                call.respond(
                    HttpStatusCode.Created,
                    user
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid user data"
                                )
                    )
                )
            }
        }

        // =====================================================
        // ADMIN — UPDATE USER
        // =====================================================

        put("/api/admin/users/{id}") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.CHANGE_USER_ROLES
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
                            "Invalid user ID"
                        )

                val request =
                    call.receive<UpdateAdminUserRequest>()

                call.respond(
                    adminUserService.updateUser(
                        id = id,
                        request = request
                    )
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid user data"
                                )
                    )
                )

            } catch (e: NoSuchElementException) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "User not found"
                    )
                )
            }
        }

        // =====================================================
        // ADMIN — DEACTIVATE USER
        // =====================================================

        put("/api/admin/users/{id}/deactivate") {

            val principal =
                call.principal<JWTPrincipal>()

            try {
                RbacGuard.require(
                    principal,
                    Permission.DEACTIVATE_USERS
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
                            "Invalid user ID"
                        )

                adminUserService.deactivateUser(id)

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "status" to "OK",
                        "message" to "User deactivated successfully"
                    )
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "status" to "ERROR",
                        "message" to (
                                e.message
                                    ?: "Invalid user ID"
                                )
                    )
                )

            } catch (e: NoSuchElementException) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "status" to "ERROR",
                        "message" to "User not found"
                    )
                )
            }
        }
    }
}

// =====================================================
// SAVED LOCATION → API RESPONSE
// =====================================================

private fun SavedLocationRecord.toResponse() =
    SavedLocationResponse(
        id = id,
        label = label,
        address = address,
        isDefault = isDefault,
        createdAt = createdAt
    )