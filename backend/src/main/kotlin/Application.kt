package com.movofeeds

import com.movofeeds.config.DatabaseConfig
import com.movofeeds.config.configureAuthentication
import com.movofeeds.database.DatabaseInitializer
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import org.slf4j.event.Level

fun Application.module(
    initializeDatabase: Boolean = true,
    initializeAuthentication: Boolean = true
) {
    install(CallLogging) {
        level = Level.INFO
    }

    if (initializeDatabase) {
        DatabaseConfig.connect()
        DatabaseInitializer.initialize()
    }

    if (initializeAuthentication) {
        configureAuthentication()
    }

    configureRouting()
}