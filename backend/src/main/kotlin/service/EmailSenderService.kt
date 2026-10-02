package com.movofeeds.service

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Sends transactional emails via Resend (https://resend.com).
 *
 * Free tier: 3,000 emails/month, 100/day.
 * Sandbox sender: onboarding@resend.dev (no domain verification needed).
 *
 * Configuration via environment variables:
 *   RESEND_API_KEY  – your Resend API key (starts with "re_")
 *   EMAIL_FROM      – optional; defaults to "onboarding@resend.dev"
 */
class EmailSenderService(
    private val apiKey: String = System.getenv("RESEND_API_KEY")
        ?: error("RESEND_API_KEY environment variable is not set"),
    private val fromAddress: String = System.getenv("EMAIL_FROM")
        ?: "MkulimaFeeds <onboarding@resend.dev>"
) {

    private val logger = LoggerFactory.getLogger(EmailSenderService::class.java)

    private val client = HttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Sends a plain-text email.
     *
     * @return true if Resend accepted the request.
     */
    suspend fun sendEmail(
        to: String,
        subject: String,
        body: String
    ): Boolean {
        return try {

            val payload = ResendEmailRequest(
                from = fromAddress,
                to = listOf(to),
                subject = subject,
                text = body
            )

            val response = client.post("https://api.resend.com/emails") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(payload))
            }

            val bodyText = response.bodyAsText()

            if (response.status.value in 200..299) {
                logger.info("Email sent to $to: $bodyText")
                true
            } else {
                logger.error("Resend error (${response.status.value}): $bodyText")
                false
            }

        } catch (e: Exception) {
            logger.error("Failed to send email to $to", e)
            false
        }
    }

    @Serializable
    private data class ResendEmailRequest(
        val from: String,
        val to: List<String>,
        val subject: String,
        val text: String
    )
}