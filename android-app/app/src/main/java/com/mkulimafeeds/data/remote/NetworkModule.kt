package com.mkulimafeeds.data.remote
import io.ktor.client.HttpClient

object NetworkModule {

    val httpClient: HttpClient by lazy {
        HttpClientFactory.create()
    }

    val apiService: ApiService by lazy {
        ApiService(httpClient)
    }
}