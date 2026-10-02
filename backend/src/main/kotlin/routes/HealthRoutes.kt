package com.movofeeds.routes

import com.movofeeds.dto.HealthResponse
import com.movofeeds.service.HealthService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.healthRoutes(
    healthService: HealthService
) {

    get("/api/health") {

        val response = HealthResponse(
            status = "OK",
            service = "MovoFeeds Backend",
            message = healthService.getHealthStatus()
        )

        call.respond(
            HttpStatusCode.OK,
            response
        )
    }
}