package com.mkulimafeeds.data.remote
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class HealthApi(
    private val client: HttpClient
) {

    suspend fun checkHealth(): String {
        return client
            .get("http://127.0.0.1:8080/api/health")
            .body()
    }
}