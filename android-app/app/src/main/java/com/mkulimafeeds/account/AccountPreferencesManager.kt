package com.mkulimafeeds.account

import android.content.Context

/**
 * Wrapper around SharedPreferences for account-related preferences.
 *
 * Stores: theme, language, notification toggles.
 *
 * All keys are strings; values can be primitives or enum-like strings.
 * The app reads these on startup and applies them where applicable.
 */
class AccountPreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // =========================================================
    // THEME
    // =========================================================

    fun getTheme(): String =
        prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

    fun setTheme(theme: String) {
        require(theme in VALID_THEMES) { "Invalid theme: $theme" }
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    // =========================================================
    // LANGUAGE
    // =========================================================

    fun getLanguage(): String =
        prefs.getString(KEY_LANGUAGE, LANGUAGE_EN) ?: LANGUAGE_EN

    fun setLanguage(language: String) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }

    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    fun areNotificationsEnabled(): Boolean =
        prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun areOrderUpdatesEnabled(): Boolean =
        prefs.getBoolean(KEY_ORDER_UPDATES, true)

    fun setOrderUpdatesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ORDER_UPDATES, enabled).apply()
    }

    fun arePromotionsEnabled(): Boolean =
        prefs.getBoolean(KEY_PROMOTIONS, false)

    fun setPromotionsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROMOTIONS, enabled).apply()
    }

    // =========================================================
    // CLEAR (used on logout)
    // =========================================================

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "account_preferences"

        private const val KEY_THEME = "theme"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_ORDER_UPDATES = "order_updates"
        private const val KEY_PROMOTIONS = "promotions"

        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
        const val THEME_SYSTEM = "SYSTEM"

        const val LANGUAGE_EN = "EN"
        const val LANGUAGE_SW = "SW"

        val VALID_THEMES = setOf(THEME_LIGHT, THEME_DARK, THEME_SYSTEM)
    }
}