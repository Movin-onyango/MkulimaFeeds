package com.mkulimafeeds.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {

    fun create(): HttpClient {
        return HttpClient(OkHttp) {

            expectSuccess = false

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        prettyPrint = false
                        encodeDefaults = true
                        explicitNulls = false
                    }
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = ApiConfig.REQUEST_TIMEOUT
                connectTimeoutMillis = ApiConfig.CONNECT_TIMEOUT
                socketTimeoutMillis = ApiConfig.SOCKET_TIMEOUT
            }
        }
    }
}