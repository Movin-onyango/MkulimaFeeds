package com.movofeeds.security
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.movofeeds.config.JwtConfig
import com.movofeeds.repository.UserRecord
import java.util.Date

object JwtService {

    private val algorithm: Algorithm
        get() = Algorithm.HMAC256(JwtConfig.secret)

    fun generateToken(user: UserRecord): String {

        val now = System.currentTimeMillis()

        return JWT.create()
            .withIssuer(JwtConfig.issuer)
            .withAudience(JwtConfig.audience)
            .withClaim("userId", user.id)
            .withClaim("email", user.email)
            .withClaim("role", user.role)
            .withIssuedAt(Date(now))
            .withExpiresAt(
                Date(now + JwtConfig.expirationMs)
            )
            .sign(algorithm)
    }
}