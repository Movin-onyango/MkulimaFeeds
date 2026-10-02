package com.movofeeds.config
object JwtConfig {

    const val issuer = "movofeeds-api"
    const val audience = "movofeeds-client"
    const val realm = "MovoFeeds"

    const val expirationMs = 1000L * 60 * 60 * 24 // 24 hours

    val secret: String
        get() = System.getenv("JWT_SECRET")
            ?: error("JWT_SECRET environment variable is not set")
}