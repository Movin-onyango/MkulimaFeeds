package com.mkulimafeeds.data.remote

/**
 * Public branding settings from the backend.
 * Fetched on demand — cached for the app session.
 *
 * All fields are strings and come from the settings table.
 * Admin can change any of them from Business Rules.
 */
data class BrandingSettings(
    val appName: String,
    val companyName: String,
    val supportEmail: String,
    val supportPhone: String,
    val whatsappNumber: String,
    val websiteUrl: String,
    val termsUrl: String,
    val privacyUrl: String,
    val aboutText: String,
    val versionLabel: String
) {
    companion object {

        /**
         * Sensible fallbacks if the backend can't be reached.
         * These match the seeded defaults.
         */
        val FALLBACK = BrandingSettings(
            appName = "MkulimaFeeds",
            companyName = "MkulimaFeeds Ltd",
            supportEmail = "support@mkulimafeeds.co.ke",
            supportPhone = "+254700000000",
            whatsappNumber = "254700000000",
            websiteUrl = "https://mkulimafeeds.co.ke",
            termsUrl = "https://mkulimafeeds.co.ke/terms",
            privacyUrl = "https://mkulimafeeds.co.ke/privacy",
            aboutText = "MkulimaFeeds is Kenya's premier supplier of high-quality animal feeds.",
            versionLabel = "1.0.0"
        )

        /**
         * Parse the raw map returned by the backend into a typed object.
         * Missing keys fall back to FALLBACK values.
         */
        fun fromMap(map: Map<String, String>): BrandingSettings {
            return BrandingSettings(
                appName = map["branding.app_name"] ?: FALLBACK.appName,
                companyName = map["branding.company_name"] ?: FALLBACK.companyName,
                supportEmail = map["branding.support_email"] ?: FALLBACK.supportEmail,
                supportPhone = map["branding.support_phone"] ?: FALLBACK.supportPhone,
                whatsappNumber = map["branding.whatsapp_number"] ?: FALLBACK.whatsappNumber,
                websiteUrl = map["branding.website_url"] ?: FALLBACK.websiteUrl,
                termsUrl = map["branding.terms_url"] ?: FALLBACK.termsUrl,
                privacyUrl = map["branding.privacy_url"] ?: FALLBACK.privacyUrl,
                aboutText = map["branding.about_text"] ?: FALLBACK.aboutText,
                versionLabel = map["branding.version_label"] ?: FALLBACK.versionLabel
            )
        }
    }
}