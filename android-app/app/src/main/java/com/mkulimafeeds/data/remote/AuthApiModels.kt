package com.mkulimafeeds.data.remote

import kotlinx.serialization.Serializable

// =========================================================
// EXISTING MODELS (kept for backward compatibility)
// =========================================================

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val phone: String,
    val email: String,
    val password: String
)

@Serializable
data class UserResponse(
    val id: Long,
    val name: String,
    val phone: String,
    val email: String,
    val role: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

@Serializable
data class AuthMeResponse(
    val status: String,
    val userId: Long,
    val email: String,
    val role: String
)

// =========================================================
// NEW: UNIFIED LOGIN
// =========================================================

@Serializable
data class UnifiedLoginRequest(
    val identifier: String,
    val password: String
)

// =========================================================
// NEW: TWO-STEP REGISTRATION
// =========================================================

@Serializable
data class RegisterInitiateRequest(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val password: String
)

@Serializable
data class RegisterInitiateResponse(
    val status: String,
    val message: String,
    val channel: String,
    val destination: String,
    val expiresInSeconds: Int
)

@Serializable
data class RegisterVerifyRequest(
    val destination: String,
    val code: String
)

// =========================================================
// NEW: FORGOT PASSWORD (used in Pass F)
// =========================================================

@Serializable
data class ForgotPasswordInitiateRequest(
    val identifier: String
)

@Serializable
data class ForgotPasswordInitiateResponse(
    val status: String,
    val message: String,
    val emailDestination: String?,
    val phoneDestination: String?
)

@Serializable
data class ForgotPasswordVerifyRequest(
    val identifier: String,
    val emailCode: String? = null,
    val phoneCode: String? = null
)

@Serializable
data class ForgotPasswordResetRequest(
    val identifier: String,
    val resetToken: String,
    val newPassword: String
)