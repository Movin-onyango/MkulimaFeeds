package com.movofeeds.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val phone: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
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
@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
// =========================================================
// REGISTRATION (two-step)
// =========================================================

@Serializable
data class RegisterInitiateRequest(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val password: String
)

@Serializable
data class RegisterVerifyRequest(
    val destination: String, // the email or phone that received the OTP
    val code: String
)

@Serializable
data class RegisterInitiateResponse(
    val status: String,
    val message: String,
    val channel: String,       // "EMAIL" or "PHONE"
    val destination: String,
    val expiresInSeconds: Int
)

// =========================================================
// LOGIN (unified: email OR phone)
// =========================================================

@Serializable
data class UnifiedLoginRequest(
    val identifier: String,    // email OR phone
    val password: String
)

// =========================================================
// FORGOT PASSWORD (two-step)
// =========================================================

@Serializable
data class ForgotPasswordInitiateRequest(
    val identifier: String     // email OR phone of an existing account
)

@Serializable
data class ForgotPasswordInitiateResponse(
    val status: String,
    val message: String,
    val emailDestination: String?,   // masked, e.g. "j***@gmail.com"
    val phoneDestination: String?    // masked, e.g. "+2547***78"
)

@Serializable
data class ForgotPasswordVerifyRequest(
    val identifier: String,          // original email or phone used to look up account
    val emailCode: String? = null,   // code received by email
    val phoneCode: String? = null    // code received by SMS
)

@Serializable
data class ForgotPasswordResetRequest(
    val identifier: String,
    val resetToken: String,          // token returned by verify step
    val newPassword: String
)