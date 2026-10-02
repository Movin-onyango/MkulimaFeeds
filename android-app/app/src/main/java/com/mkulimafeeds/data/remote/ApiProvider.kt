package com.mkulimafeeds.data.remote
object ApiProvider {

    val client = HttpClientFactory.create()

    val healthApi = HealthApi(client)
}