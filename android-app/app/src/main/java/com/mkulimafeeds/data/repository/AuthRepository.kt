package com.mkulimafeeds.data.repository

import com.mkulimafeeds.data.local.TokenManager
import com.mkulimafeeds.data.remote.ApiService
import com.mkulimafeeds.data.remote.AuthMeResponse
import com.mkulimafeeds.data.remote.AuthResponse
import com.mkulimafeeds.data.remote.ForgotPasswordInitiateResponse
import com.mkulimafeeds.data.remote.RegisterInitiateRequest
import com.mkulimafeeds.data.remote.RegisterInitiateResponse

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    // =========================================================
    // LOGIN (email OR phone)
    // =========================================================

    suspend fun login(
        identifier: String,
        password: String
    ): AuthResponse {
        val response = apiService.login(
            identifier = identifier,
            password = password
        )
        tokenManager.saveToken(response.token)
        return response
    }

    // =========================================================
    // TWO-STEP REGISTRATION
    // =========================================================

    /**
     * Step 1: initiate registration.
     * Sends an OTP to the provided email or phone.
     * Does NOT create the account yet.
     */
    suspend fun registerInitiate(
        name: String,
        email: String?,
        phone: String?,
        password: String
    ): RegisterInitiateResponse {
        return apiService.registerInitiate(
            RegisterInitiateRequest(
                name = name,
                email = email,
                phone = phone,
                password = password
            )
        )
    }

    /**
     * Step 2: verify the OTP.
     * On success, the account is created and a JWT is returned.
     * The token is saved automatically.
     */
    suspend fun registerVerify(
        destination: String,
        code: String
    ): AuthResponse {
        val response = apiService.registerVerify(
            destination = destination,
            code = code
        )
        tokenManager.saveToken(response.token)
        return response
    }

    // =========================================================
    // FORGOT PASSWORD (used in Pass F)
    // =========================================================

    suspend fun forgotPasswordInitiate(
        identifier: String
    ): ForgotPasswordInitiateResponse {
        return apiService.forgotPasswordInitiate(identifier)
    }

    suspend fun forgotPasswordVerify(
        identifier: String,
        emailCode: String?,
        phoneCode: String?
    ): Map<String, String> {
        return apiService.forgotPasswordVerify(
            identifier = identifier,
            emailCode = emailCode,
            phoneCode = phoneCode
        )
    }

    suspend fun forgotPasswordReset(
        identifier: String,
        resetToken: String,
        newPassword: String
    ) {
        apiService.forgotPasswordReset(
            identifier = identifier,
            resetToken = resetToken,
            newPassword = newPassword
        )
    }

    // =========================================================
    // SESSION HELPERS
    // =========================================================

    suspend fun getCurrentUser(): AuthMeResponse {
        val token = tokenManager.getToken()
            ?: throw IllegalArgumentException("No authentication token")
        return apiService.getCurrentUser(token)
    }

    fun getToken(): String? = tokenManager.getToken()

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    fun logout() {
        tokenManager.clearToken()
    }
}