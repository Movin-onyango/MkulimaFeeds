package com.mkulimafeeds.data.remote
import com.mkulimafeeds.BuildConfig
object ApiConfig {

    // Physical Android device using adb reverse
    const val BASE_URL = "http://127.0.0.1:8080/"
    const val BASE_URL = BuildConfig.BASE_URL
    const val CONNECT_TIMEOUT = 15_000L
    const val REQUEST_TIMEOUT = 30_000L
    const val SOCKET_TIMEOUT = 30_000L

    const val AUTH_HEADER = "Authorization"
    const val CONTENT_TYPE = "application/json"
}