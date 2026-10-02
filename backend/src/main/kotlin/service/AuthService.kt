package com.movofeeds.service

import com.movofeeds.repository.UserRecord
import com.movofeeds.repository.UserRepository
import com.movofeeds.security.EmailValidator
import com.movofeeds.security.PasswordHasher
import com.movofeeds.security.PhoneValidator
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.runBlocking

class AuthService(
    private val userRepository: UserRepository = UserRepository(),
    private val verificationService: VerificationService = VerificationService()
) {

    // =========================================================
    // RESET TOKEN STORAGE (in-memory, per process)
    // =========================================================

    private data class ResetToken(
        val userId: Long,
        val createdAt: Long
    )

    private val resetTokens =
        ConcurrentHashMap<String, ResetToken>()

    private val RESET_TOKEN_TTL_MS = 5L * 60L * 1000L // 5 minutes

    // =========================================================
    // LEGACY REGISTER (kept for backward compatibility)
    // =========================================================

    fun register(
        name: String,
        phone: String,
        email: String,
        password: String
    ): UserRecord {
        require(name.isNotBlank()) { "Name is required" }
        require(phone.isNotBlank()) { "Phone number is required" }
        require(email.isNotBlank()) { "Email is required" }
        require(password.length >= 8) {
            "Password must be at least 8 characters"
        }
        require(EmailValidator.isValid(email)) {
            EmailValidator.rejectionReason(email) ?: "Invalid email"
        }
        require(PhoneValidator.isValid(phone)) {
            PhoneValidator.rejectionReason(phone) ?: "Invalid phone"
        }
        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("Email is already registered")
        }
        if (userRepository.existsByPhone(phone)) {
            throw IllegalArgumentException("Phone number is already registered")
        }
        val passwordHash = PasswordHasher.hash(password)
        return userRepository.createUser(
            name = name,
            phone = phone,
            email = email,
            passwordHash = passwordHash,
            role = "CUSTOMER"
        )
    }

    // =========================================================
    // STEP 1: INITIATE REGISTRATION
    // =========================================================

    fun initiateRegistration(
        name: String,
        email: String?,
        phone: String?,
        password: String
    ): Result<Pair<String, String>> {
        // returns (channel, destination)

        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Name is required")
            )
        }
        if (trimmedName.length < 2) {
            return Result.failure(
                IllegalArgumentException("Name must be at least 2 characters")
            )
        }

        if (password.length < 8) {
            return Result.failure(
                IllegalArgumentException("Password must be at least 8 characters")
            )
        }

        val hasEmail = !email.isNullOrBlank()
        val hasPhone = !phone.isNullOrBlank()

        if (!hasEmail && !hasPhone) {
            return Result.failure(
                IllegalArgumentException(
                    "Please provide an email address or a phone number"
                )
            )
        }
        if (hasEmail && hasPhone) {
            return Result.failure(
                IllegalArgumentException(
                    "Please provide either an email address or a phone number, not both"
                )
            )
        }

        val channel: String
        val destination: String

        if (hasEmail) {
            val normalized = EmailValidator.normalize(email!!)
            val reason = EmailValidator.rejectionReason(normalized)
            if (reason != null) {
                return Result.failure(IllegalArgumentException(reason))
            }
            channel = VerificationService.CHANNEL_EMAIL
            destination = normalized
        } else {
            val reason = PhoneValidator.rejectionReason(phone!!)
            if (reason != null) {
                return Result.failure(IllegalArgumentException(reason))
            }
            channel = VerificationService.CHANNEL_PHONE
            destination = PhoneValidator.normalize(phone)
        }

        if (hasEmail && userRepository.existsByEmail(destination)) {
            return Result.failure(
                IllegalArgumentException("This email is already registered")
            )
        }
        if (hasPhone && userRepository.existsByPhone(destination)) {
            return Result.failure(
                IllegalArgumentException("This phone number is already registered")
            )
        }

        val passwordHash = PasswordHasher.hash(password)

        AuthPendingStorage.save(
            AuthPendingStorage.PendingRegistration(
                name = trimmedName,
                email = if (hasEmail) destination else null,
                phone = if (hasPhone) destination else null,
                passwordHash = passwordHash,
                channel = channel,
                destination = destination,
                createdAt = System.currentTimeMillis()
            )
        )

        val sendResult = runBlocking {
            verificationService.sendVerificationCode(
                userId = null,
                purpose = VerificationService.PURPOSE_REGISTER,
                channel = channel,
                destination = destination
            )
        }

        return if (sendResult.isSuccess) {
            Result.success(channel to destination)
        } else {
            sendResult.exceptionOrNull()?.let {
                Result.failure(it)
            } ?: Result.failure(
                IllegalStateException("Failed to send code")
            )
        }
    }

    // =========================================================
    // STEP 2: VERIFY REGISTRATION CODE
    // =========================================================

    fun verifyRegistration(
        destination: String,
        code: String
    ): Result<UserRecord> {

        val normalized = destination.trim().lowercase()

        val pending = AuthPendingStorage.get(normalized)
            ?: return Result.failure(
                IllegalArgumentException(
                    "No pending registration found. Please start over."
                )
            )

        val verifyResult = verificationService.verifyCode(
            destination = pending.destination,
            purpose = VerificationService.PURPOSE_REGISTER,
            channel = pending.channel,
            code = code
        )

        if (verifyResult.isFailure) {
            return Result.failure(
                verifyResult.exceptionOrNull()
                    ?: IllegalArgumentException("Invalid code")
            )
        }

        try {
            val user = userRepository.createUser(
                name = pending.name,
                phone = pending.phone ?: "",
                email = pending.email ?: "",
                passwordHash = pending.passwordHash,
                role = "CUSTOMER"
            )

            AuthPendingStorage.remove(normalized)

            return Result.success(user)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    // =========================================================
    // UNIFIED LOGIN (email OR phone)
    // =========================================================

    fun loginWithIdentifier(
        identifier: String,
        password: String
    ): UserRecord {

        val trimmed = identifier.trim()

        // Try email first
        var user: UserRecord? = null
        if (trimmed.contains("@")) {
            val normalized = EmailValidator.normalize(trimmed)
            user = userRepository.findByEmail(normalized)
        } else {
            val normalized = PhoneValidator.normalize(trimmed)
            user = userRepository.findByPhone(normalized)
        }

        val resolved = user
            ?: throw IllegalArgumentException("Invalid credentials")

        if (!resolved.isActive) {
            throw IllegalArgumentException("User account is inactive")
        }
        if (resolved.status.equals("SUSPENDED", ignoreCase = true)) {
            throw IllegalArgumentException(
                "Your account has been suspended. Contact support for assistance."
            )
        }
        if (resolved.status.equals("PENDING", ignoreCase = true)) {
            throw IllegalArgumentException(
                "Your account is pending approval. Please wait for admin review."
            )
        }

        if (!PasswordHasher.verify(password, resolved.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        return resolved
    }

    // =========================================================
    // LEGACY LOGIN (kept for backward compatibility)
    // =========================================================

    fun login(
        email: String,
        password: String
    ): UserRecord {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Invalid email or password")

        if (!user.isActive) {
            throw IllegalArgumentException("User account is inactive")
        }
        if (user.status.equals("SUSPENDED", ignoreCase = true)) {
            throw IllegalArgumentException(
                "Your account has been suspended. Contact support for assistance."
            )
        }

        if (!PasswordHasher.verify(password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password")
        }
        return user
    }

    // =========================================================
    // CHANGE PASSWORD (authenticated)
    // =========================================================

    fun changePassword(
        userId: Long,
        currentPassword: String,
        newPassword: String
    ) {
        require(userId > 0) { "Invalid user ID" }
        require(currentPassword.isNotBlank()) {
            "Current password is required"
        }
        require(newPassword.isNotBlank()) {
            "New password is required"
        }
        require(newPassword.length >= 8) {
            "New password must be at least 8 characters"
        }
        require(currentPassword != newPassword) {
            "New password must be different from the current password"
        }
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User account not found")
        if (!user.isActive) {
            throw IllegalArgumentException("User account is inactive")
        }
        if (!PasswordHasher.verify(currentPassword, user.passwordHash)) {
            throw IllegalArgumentException("Current password is incorrect")
        }
        val newPasswordHash = PasswordHasher.hash(newPassword)
        val updated = userRepository.changePassword(
            id = userId,
            passwordHash = newPasswordHash
        )
        if (!updated) {
            throw IllegalStateException("Failed to update password")
        }
    }

    // =========================================================
    // FORGOT PASSWORD — STEP 1: INITIATE
    // =========================================================

    data class ForgotPasswordInitiateResult(
        val emailDestination: String?,
        val phoneDestination: String?
    )

    fun initiatePasswordReset(
        identifier: String
    ): Result<ForgotPasswordInitiateResult> {

        val trimmed = identifier.trim()

        val user: UserRecord? = if (trimmed.contains("@")) {
            userRepository.findByEmail(EmailValidator.normalize(trimmed))
        } else {
            userRepository.findByPhone(PhoneValidator.normalize(trimmed))
        }

        if (user == null) {
            return Result.success(
                ForgotPasswordInitiateResult(
                    emailDestination = null,
                    phoneDestination = null
                )
            )
        }

        val email = user.email.trim().ifBlank { null }
        val phone = user.phone.trim().ifBlank { null }

        if (email == null && phone == null) {
            return Result.success(
                ForgotPasswordInitiateResult(
                    emailDestination = null,
                    phoneDestination = null
                )
            )
        }

        if (email != null) {
            runBlocking {
                verificationService.sendVerificationCode(
                    userId = user.id,
                    purpose = VerificationService.PURPOSE_PASSWORD_RESET,
                    channel = VerificationService.CHANNEL_EMAIL,
                    destination = email
                )
            }
        }

        if (phone != null) {
            runBlocking {
                verificationService.sendVerificationCode(
                    userId = user.id,
                    purpose = VerificationService.PURPOSE_PASSWORD_RESET,
                    channel = VerificationService.CHANNEL_PHONE,
                    destination = phone
                )
            }
        }

        return Result.success(
            ForgotPasswordInitiateResult(
                emailDestination = email?.let { maskEmail(it) },
                phoneDestination = phone?.let { maskPhone(it) }
            )
        )
    }

    // =========================================================
    // FORGOT PASSWORD — STEP 2: VERIFY BOTH CODES
    // =========================================================

    fun verifyPasswordResetCodes(
        identifier: String,
        emailCode: String?,
        phoneCode: String?
    ): Result<String> {

        val trimmed = identifier.trim()

        val user: UserRecord? = if (trimmed.contains("@")) {
            userRepository.findByEmail(EmailValidator.normalize(trimmed))
        } else {
            userRepository.findByPhone(PhoneValidator.normalize(trimmed))
        }

        if (user == null) {
            return Result.failure(
                IllegalArgumentException("Invalid code")
            )
        }

        val email = user.email.trim().ifBlank { null }
        val phone = user.phone.trim().ifBlank { null }

        if (email != null) {
            if (emailCode.isNullOrBlank()) {
                return Result.failure(
                    IllegalArgumentException("Email verification code is required")
                )
            }
            val emailResult = verificationService.verifyCode(
                destination = email,
                purpose = VerificationService.PURPOSE_PASSWORD_RESET,
                channel = VerificationService.CHANNEL_EMAIL,
                code = emailCode
            )
            if (emailResult.isFailure) {
                return Result.failure(
                    emailResult.exceptionOrNull()
                        ?: IllegalArgumentException("Invalid email code")
                )
            }
        }

        if (phone != null) {
            if (phoneCode.isNullOrBlank()) {
                return Result.failure(
                    IllegalArgumentException("Phone verification code is required")
                )
            }
            val phoneResult = verificationService.verifyCode(
                destination = phone,
                purpose = VerificationService.PURPOSE_PASSWORD_RESET,
                channel = VerificationService.CHANNEL_PHONE,
                code = phoneCode
            )
            if (phoneResult.isFailure) {
                return Result.failure(
                    phoneResult.exceptionOrNull()
                        ?: IllegalArgumentException("Invalid phone code")
                )
            }
        }

        val token = UUID.randomUUID().toString()
        resetTokens[token] = ResetToken(
            userId = user.id,
            createdAt = System.currentTimeMillis()
        )

        return Result.success(token)
    }

    // =========================================================
    // FORGOT PASSWORD — STEP 3: RESET WITH TOKEN
    // =========================================================

    fun resetPasswordWithToken(
        identifier: String,
        resetToken: String,
        newPassword: String
    ): Result<Unit> {

        if (newPassword.length < 8) {
            return Result.failure(
                IllegalArgumentException("Password must be at least 8 characters")
            )
        }

        val entry = resetTokens[resetToken]
            ?: return Result.failure(
                IllegalArgumentException("Invalid or expired reset token")
            )

        if (System.currentTimeMillis() - entry.createdAt > RESET_TOKEN_TTL_MS) {
            resetTokens.remove(resetToken)
            return Result.failure(
                IllegalArgumentException("Reset token has expired")
            )
        }

        val trimmed = identifier.trim()
        val user: UserRecord? = if (trimmed.contains("@")) {
            userRepository.findByEmail(EmailValidator.normalize(trimmed))
        } else {
            userRepository.findByPhone(PhoneValidator.normalize(trimmed))
        }

        if (user == null || user.id != entry.userId) {
            resetTokens.remove(resetToken)
            return Result.failure(
                IllegalArgumentException("Invalid reset token")
            )
        }

        val newHash = PasswordHasher.hash(newPassword)
        val updated = userRepository.changePassword(
            id = user.id,
            passwordHash = newHash
        )

        resetTokens.remove(resetToken)

        return if (updated) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed to update password"))
        }
    }

    // =========================================================
    // MASKING HELPERS
    // =========================================================

    private fun maskEmail(email: String): String {
        val at = email.indexOf('@')
        if (at <= 0) return "***"
        val local = email.substring(0, at)
        val domain = email.substring(at)
        val visibleLocal = local.take(1)
        return "$visibleLocal***$domain"
    }

    private fun maskPhone(phone: String): String {
        if (phone.length < 6) return "***"
        return phone.take(6) + "***" + phone.takeLast(2)
    }
}