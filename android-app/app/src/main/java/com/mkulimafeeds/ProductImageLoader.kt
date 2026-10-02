package com.mkulimafeeds

import android.graphics.BitmapFactory
import android.widget.ImageView
import com.mkulimafeeds.data.remote.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object ProductImageLoader {

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

    fun load(
        imageView: ImageView,
        imagePath: String?
    ) {

        imageView.setImageResource(
            android.R.drawable.ic_menu_gallery
        )

        if (imagePath.isNullOrBlank()) {
            return
        }

        val fullUrl =
            if (imagePath.startsWith("http")) {
                imagePath
            } else {
                ApiConfig.BASE_URL
                    .trimEnd('/') +
                        "/" +
                        imagePath.trimStart('/')
            }

        imageView.tag = fullUrl

        scope.launch {

            var connection:
                    HttpURLConnection? = null

            try {

                connection =
                    URL(fullUrl)
                        .openConnection()
                            as HttpURLConnection

                connection.connectTimeout = 15_000
                connection.readTimeout = 20_000
                connection.requestMethod = "GET"
                connection.connect()

                if (
                    connection.responseCode !in
                    200..299
                ) {
                    return@launch
                }

                val bitmap =
                    connection
                        .inputStream
                        .use {
                            BitmapFactory.decodeStream(it)
                        }

                withContext(Dispatchers.Main) {

                    if (
                        imageView.tag == fullUrl &&
                        bitmap != null
                    ) {
                        imageView.setImageBitmap(bitmap)
                    }
                }

            } catch (_: Exception) {

                // Keep placeholder.

            } finally {
                connection?.disconnect()
            }
        }
    }
}