package com.movofeeds.service

import org.slf4j.LoggerFactory

/**
 * Dispatcher for email and SMS.
 *
 * Checks environment variables at startup:
 *   - If RESEND_API_KEY is set → send via Resend
 *   - If AT_API_KEY is set     → send via Africa's Talking
 *   - Otherwise                → log to console (fallback for dev)
 */
object MockSenderService {

    private val logger = LoggerFactory.getLogger(MockSenderService::class.java)

    private val emailSender: EmailSenderService? by lazy {
        try {
            if (System.getenv("RESEND_API_KEY").isNullOrBlank()) {
                null
            } else {
                EmailSenderService()
            }
        } catch (e: Exception) {
            logger.warn("Resend not configured, falling back to console")
            null
        }
    }

    private val smsSender: SmsSenderService? by lazy {
        try {
            if (System.getenv("AT_API_KEY").isNullOrBlank()) {
                null
            } else {
                SmsSenderService()
            }
        } catch (e: Exception) {
            logger.warn("Africa's Talking not configured, falling back to console")
            null
        }
    }

    /**
     * Sends an email. Uses Resend if configured, otherwise console log.
     */
    suspend fun sendEmail(
        toAddress: String,
        subject: String,
        code: String
    ): Boolean {

        val body = """
            Your MkulimaFeeds verification code is: $code

            This code expires in 10 minutes. If you didn't request this, you can ignore this email.
        """.trimIndent()

        val sender = emailSender
        if (sender != null) {
            return sender.sendEmail(
                to = toAddress,
                subject = subject,
                body = body
            )
        }

        logger.info(
            """
            ===============================
              MOCK EMAIL
            ===============================
            To:      $toAddress
            Subject: $subject
            Code:    $code
            Expires: 10 minutes
            ===============================
            """.trimIndent()
        )
        return true
    }

    /**
     * Sends an SMS. Uses Africa's Talking if configured, otherwise console log.
     */
    suspend fun sendSms(
        toPhone: String,
        message: String,
        code: String
    ): Boolean {

        val body = "$message $code. Expires in 10 minutes."

        val sender = smsSender
        if (sender != null) {
            return sender.sendSms(
                toPhone = toPhone,
                message = body
            )
        }

        logger.info(
            """
            ===============================
              MOCK SMS
            ===============================
            To:      $toPhone
            Message: $message
            Code:    $code
            Expires: 10 minutes
            ===============================
            """.trimIndent()
        )
        return true
    }
}