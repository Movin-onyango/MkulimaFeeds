package com.movofeeds.config

import org.jetbrains.exposed.sql.Database

object DatabaseConfig {

    fun connect() {

        val host = System.getenv("DB_HOST") ?: "localhost"
        val port = System.getenv("DB_PORT") ?: "5432"
        val database = System.getenv("DB_NAME") ?: "movofeeds_db"
        val user = System.getenv("DB_USER") ?: "movofeeds_app"
        val password = System.getenv("DB_PASSWORD")
            ?: error("DB_PASSWORD environment variable is not set")

        val jdbcUrl =
            "jdbc:postgresql://$host:$port/$database"

        Database.connect(
            url = jdbcUrl,
            driver = "org.postgresql.Driver",
            user = user,
            password = password
        )
    }
}