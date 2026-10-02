package com.movofeeds.service

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.ContentType
import org.slf4j.LoggerFactory

/**
 * Sends SMS via Africa's Talking (https://africastalking.com).
 *
 * In SANDBOX mode (free), messages appear in the AT simulator at:
 *   https://simulator.africastalking.com:1517/
 *
 * To go live, change AT_USERNAME to your live app username and
 * AT_API_KEY to your production API key. Prices in Kenya are ~KES 0.80/SMS.
 *
 * Configuration via environment variables:
 *   AT_API_KEY   – your Africa's Talking API key
 *   AT_USERNAME  – "sandbox" for dev, your app username for production
 *   AT_SENDER_ID – optional; short code or alphanumeric sender ID
 */
class SmsSenderService(
    private val apiKey: String = System.getenv("AT_API_KEY")
        ?: error("AT_API_KEY environment variable is not set"),
    private val username: String = System.getenv("AT_USERNAME")
        ?: "sandbox",
    private val senderId: String? = System.getenv("AT_SENDER_ID")
) {

    private val logger = LoggerFactory.getLogger(SmsSenderService::class.java)

    private val client = HttpClient()

    private val baseUrl: String
        get() = if (username == "sandbox") {
            "https://api.sandbox.africastalking.com/version1/messaging"
        } else {
            "https://api.africastalking.com/version1/messaging"
        }

    /**
     * Sends an SMS via Africa's Talking.
     *
     * @param toPhone E.164 format: +2547XXXXXXXX
     * @return true if AT accepted the request.
     */
    suspend fun sendSms(
        toPhone: String,
        message: String
    ): Boolean {
        return try {

            val params = Parameters.build {
                append("username", username)
                append("to", toPhone)
                append("message", message)
                senderId?.let { append("from", it) }
            }

            val response = client.submitForm(
                url = baseUrl,
                formParameters = params
            ) {
                header("apiKey", apiKey)
                header("Accept", "application/json")
            }

            val bodyText = response.bodyAsText()

            if (response.status.value in 200..299) {
                logger.info("SMS sent to $toPhone: $bodyText")
                true
            } else {
                logger.error("Africa's Talking error (${response.status.value}): $bodyText")
                false
            }

        } catch (e: Exception) {
            logger.error("Failed to send SMS to $toPhone", e)
            false
        }
    }
}