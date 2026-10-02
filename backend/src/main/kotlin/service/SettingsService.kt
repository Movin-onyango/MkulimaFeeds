package com.movofeeds.service

import com.movofeeds.models.SettingResponse
import com.movofeeds.models.SettingUpdate
import com.movofeeds.repository.SettingRecord
import com.movofeeds.repository.SettingsRepository
import java.math.BigDecimal

class SettingsService(
    private val repository: SettingsRepository = SettingsRepository()
) {

    companion object {
        /**
         * Enum options per setting key.
         * When a new ENUM setting is added, register it here.
         */
        private val ENUM_OPTIONS: Map<String, List<String>> = mapOf(
            "bulk_order.payment_terms" to listOf(
                "PREPAID",
                "NET_30",
                "NET_60"
            )
        )

        private val VALID_TYPES = setOf(
            "INT", "DECIMAL", "STRING", "BOOLEAN", "ENUM"
        )

        /**
         * Factory default values for every setting.
         * Used by the admin "Reset to Defaults" feature.
         *
         * If a new setting is added to the DB, add its default here too.
         * Missing keys are silently skipped during reset.
         */
        val DEFAULTS: Map<String, String> = mapOf(
            // =========================================================
            // BULK ORDER
            // =========================================================
            "bulk_order.min_value" to "25000",
            "bulk_order.min_qty_per_item" to "100",
            "bulk_order.max_qty_per_item" to "5000",
            "bulk_order.lead_time_days" to "3",
            "bulk_order.payment_terms" to "PREPAID",

            // =========================================================
            // DEALER
            // =========================================================
            "dealer.auto_approve" to "false",
            "dealer.min_completed_orders" to "0",
            "dealer.min_account_age_days" to "0",
            "dealer.requires_admin_approval" to "true",

            // Tier thresholds
            "dealer.tier_bronze_threshold" to "0",
            "dealer.tier_silver_threshold" to "100",
            "dealer.tier_gold_threshold" to "500",
            "dealer.tier_platinum_threshold" to "2000",

            // Tier labels
            "dealer.tier_bronze_label" to "BRONZE",
            "dealer.tier_silver_label" to "SILVER",
            "dealer.tier_gold_label" to "GOLD",
            "dealer.tier_platinum_label" to "PLATINUM",

            // =========================================================
            // RFQ (added later, but defaults prepared)
            // =========================================================
            "rfq.auto_quote_threshold" to "500000",
            "rfq.expiry_days" to "7",
            "rfq.min_lead_time_days" to "3",
            "rfq.allow_partial_acceptance" to "false",

            // =========================================================
            // CREDIT (added later, but defaults prepared)
            // =========================================================
            "credit.default_credit_limit" to "0",
            "credit.min_credit_limit" to "50000",
            "credit.max_credit_limit" to "5000000",
            "credit.default_payment_terms" to "PREPAID",
            "credit.overdue_grace_days" to "7",
            "credit.auto_suspend_on_overdue" to "true",

            // =========================================================
            // BRANDING
            // =========================================================
            "branding.app_name" to "MkulimaFeeds",
            "branding.company_name" to "MkulimaFeeds Ltd",
            "branding.support_email" to "support@mkulimafeeds.co.ke",
            "branding.support_phone" to "+254700000000",
            "branding.whatsapp_number" to "254700000000",
            "branding.website_url" to "https://mkulimafeeds.co.ke",
            "branding.terms_url" to "https://mkulimafeeds.co.ke/terms",
            "branding.privacy_url" to "https://mkulimafeeds.co.ke/privacy",
            "branding.about_text" to "MkulimaFeeds is Kenya's premier supplier of high-quality animal feeds, supplements, and agricultural supplies. We serve farmers, dealers, and agripreneurs nationwide.",
            "branding.version_label" to "1.0.0"
        )
    }

    // =========================================================
    // READ
    // =========================================================

    fun getAllSettings(): List<SettingResponse> =
        repository.findAll().map { it.toResponse() }

    fun getSettingsByCategory(category: String): List<SettingResponse> =
        repository
            .findByCategory(category.uppercase())
            .map { it.toResponse() }

    /**
     * Typed getters. Use these from other services.
     */

    fun getInt(key: String, default: Int): Int {
        val record = repository.findByKey(key) ?: return default
        return record.value.toIntOrNull() ?: default
    }

    fun getDecimal(key: String, default: BigDecimal): BigDecimal {
        val record = repository.findByKey(key) ?: return default
        return record.value.toBigDecimalOrNull() ?: default
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        val record = repository.findByKey(key) ?: return default
        return record.value.toBooleanStrictOrNull() ?: default
    }

    fun getString(key: String, default: String): String {
        return repository.findByKey(key)?.value ?: default
    }

    // =========================================================
    // UPDATE (ADMIN ONLY)
    // =========================================================

    fun updateSettings(
        changes: List<SettingUpdate>,
        adminId: Long
    ): Result<List<SettingResponse>> {

        if (changes.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("No changes provided")
            )
        }

        // ---------------------------------------------
        // Validate every change before touching the DB
        // ---------------------------------------------
        val validated = mutableListOf<Pair<String, String>>()

        for (change in changes) {
            val record = repository.findByKey(change.key)
                ?: return Result.failure(
                    IllegalArgumentException(
                        "Unknown setting: ${change.key}"
                    )
                )

            val validationError = validateValue(
                value = change.value,
                valueType = record.valueType,
                key = record.key
            )

            if (validationError != null) {
                return Result.failure(
                    IllegalArgumentException(
                        "Invalid value for ${change.key}: $validationError"
                    )
                )
            }

            validated.add(change.key to change.value.trim())
        }

        // ---------------------------------------------
        // Apply all changes atomically
        // ---------------------------------------------
        val updated = repository.updateAll(validated, adminId)
        return Result.success(updated.map { it.toResponse() })
    }

    // =========================================================
    // RESET TO FACTORY DEFAULTS (ADMIN ONLY)
    // =========================================================

    /**
     * Reset all settings to their factory defaults.
     *
     * Only updates keys that:
     *   1. Exist in DEFAULTS map
     *   2. Exist in the settings table
     *
     * Returns the list of settings after reset.
     */
    fun resetToDefaults(adminId: Long): Result<List<SettingResponse>> {
        try {
            val allSettings = repository.findAll()

            if (allSettings.isEmpty()) {
                return Result.success(emptyList())
            }

            // Build the update list — only for keys we have defaults for
            val updates = allSettings.mapNotNull { setting ->
                val defaultValue = DEFAULTS[setting.key]
                if (defaultValue != null) {
                    setting.key to defaultValue
                } else {
                    null
                }
            }

            if (updates.isEmpty()) {
                return Result.failure(
                    IllegalStateException(
                        "No default values found for any registered settings"
                    )
                )
            }

            // Apply atomically
            repository.updateAll(updates, adminId)

            // Return the full refreshed list
            return Result.success(
                repository.findAll().map { it.toResponse() }
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private fun validateValue(
        value: String,
        valueType: String,
        key: String
    ): String? {

        val trimmed = value.trim()

        if (trimmed.isEmpty()) {
            return "Value cannot be empty"
        }

        if (valueType !in VALID_TYPES) {
            return "Unknown value type: $valueType"
        }

        return when (valueType) {
            "INT" -> {
                val parsed = trimmed.toIntOrNull()
                    ?: return "Must be a whole number"
                if (parsed < 0) "Cannot be negative" else null
            }
            "DECIMAL" -> {
                val parsed = trimmed.toBigDecimalOrNull()
                    ?: return "Must be a number"
                if (parsed < BigDecimal.ZERO) "Cannot be negative" else null
            }
            "BOOLEAN" -> {
                if (trimmed.toBooleanStrictOrNull() == null) {
                    "Must be true or false"
                } else null
            }
            "ENUM" -> {
                val options = ENUM_OPTIONS[key]
                    ?: return "No enum options registered for this setting"
                if (trimmed !in options) {
                    "Must be one of: ${options.joinToString(", ")}"
                } else null
            }
            "STRING" -> null
            else -> "Unknown value type"
        }
    }

    // =========================================================
    // MAPPING
    // =========================================================

    private fun SettingRecord.toResponse(): SettingResponse =
        SettingResponse(
            key = key,
            value = value,
            valueType = valueType,
            category = category,
            description = description,
            enumOptions = ENUM_OPTIONS[key],
            updatedAt = updatedAt
        )
}