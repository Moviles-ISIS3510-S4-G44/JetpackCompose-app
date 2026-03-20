package com.university.marketplace.data.auth

import android.content.Context

class AuthSessionStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveAccessToken(token: String) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }

    fun getAccessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

    fun clear() {
        preferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "marketplace_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}
