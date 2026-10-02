package com.movofeeds.security
import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordHasher {

    fun hash(password: String): String {
        require(password.isNotBlank()) {
            "Password cannot be blank"
        }

        return BCrypt.withDefaults()
            .hashToString(12, password.toCharArray())
    }

    fun verify(
        password: String,
        passwordHash: String
    ): Boolean {
        if (password.isBlank() || passwordHash.isBlank()) {
            return false
        }

        return BCrypt.verifyer()
            .verify(
                password.toCharArray(),
                passwordHash
            )
            .verified
    }
}