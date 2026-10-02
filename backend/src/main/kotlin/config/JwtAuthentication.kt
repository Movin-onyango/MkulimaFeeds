package com.movofeeds.config
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.application.*
fun Application.configureAuthentication() {

    install(Authentication) {

        jwt("auth-jwt") {

            realm = JwtConfig.realm

            verifier(
                JWT
                    .require(
                        Algorithm.HMAC256(JwtConfig.secret)
                    )
                    .withIssuer(JwtConfig.issuer)
                    .withAudience(JwtConfig.audience)
                    .build()
            )

            validate { credential ->

                val userId = credential.payload
                    .getClaim("userId")
                    .asLong()

                val email = credential.payload
                    .getClaim("email")
                    .asString()

                val role = credential.payload
                    .getClaim("role")
                    .asString()

                if (
                    userId != null &&
                    !email.isNullOrBlank() &&
                    !role.isNullOrBlank()
                ) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}