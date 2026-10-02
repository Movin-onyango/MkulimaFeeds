package com.movofeeds
import com.movofeeds.routes.analyticsRoutes
import com.movofeeds.service.AnalyticsService
import com.movofeeds.config.DatabaseConfig
import com.movofeeds.config.configureAuthentication
import com.movofeeds.database.DatabaseInitializer
import io.ktor.server.application.*

fun Application.module(
    initializeDatabase: Boolean = true,
    initializeAuthentication: Boolean = true
) {

    if (initializeDatabase) {
        DatabaseConfig.connect()
        DatabaseInitializer.initialize()
    }

    if (initializeAuthentication) {
        configureAuthentication()
    }

    configureRouting()
}