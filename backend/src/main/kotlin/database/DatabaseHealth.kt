package com.movofeeds.database

import org.jetbrains.exposed.sql.Database
import java.sql.DriverManager

fun checkDatabaseConnection(): Boolean {
    val host = System.getenv("DB_HOST") ?: "localhost"
    val port = System.getenv("DB_PORT") ?: "5432"
    val database = System.getenv("DB_NAME") ?: "movofeeds_db"
    val user = System.getenv("DB_USER") ?: "movofeeds_app"
    val password = System.getenv("DB_PASSWORD")
        ?: error("DB_PASSWORD environment variable is not set")

    val url = "jdbc:postgresql://$host:$port/$database"

    DriverManager.getConnection(url, user, password).use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT 1").use { result ->
                return result.next() && result.getInt(1) == 1
            }
        }
    }
}