package com.mkulimafeeds.data.local

import android.content.Context

class TokenManager(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "mkulima_auth",
            Context.MODE_PRIVATE
        )

    fun saveToken(token: String) {
        preferences
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun getToken(): String? {
        return preferences.getString(
            KEY_TOKEN,
            null
        )
    }

    fun clearToken() {
        preferences
            .edit()
            .remove(KEY_TOKEN)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
    }
}