package com.movofeeds

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerTest {

    @Test
    fun testRootEndpoint() = testApplication {

        System.setProperty(
            "JWT_SECRET",
            "test-secret-for-unit-tests-only"
        )

        application {
            module(
                initializeDatabase = false,
                initializeAuthentication = true
            )
        }

        val response = client.get("/")

        assertEquals(
            HttpStatusCode.OK,
            response.status
        )
    }
}