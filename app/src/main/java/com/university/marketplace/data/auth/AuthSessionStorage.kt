package com.university.marketplace.data.auth

import android.content.Context

data class AuthSession(
    val sessionId: String?,
    val accessToken: String,
    val accessExpiresAtEpochMillis: Long?,
    val refreshToken: String?,
    val refreshExpiresAtEpochMillis: Long?
)

class AuthSessionStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveSession(session: AuthSession, rememberAcrossRestarts: Boolean) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_SESSION_ID, session.sessionId)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putBoolean(KEY_REMEMBER_ME, rememberAcrossRestarts)
            .apply {
                if (session.accessExpiresAtEpochMillis != null) {
                    putLong(KEY_ACCESS_EXPIRES_AT, session.accessExpiresAtEpochMillis)
                } else {
                    remove(KEY_ACCESS_EXPIRES_AT)
                }
                if (session.refreshExpiresAtEpochMillis != null) {
                    putLong(KEY_REFRESH_EXPIRES_AT, session.refreshExpiresAtEpochMillis)
                } else {
                    remove(KEY_REFRESH_EXPIRES_AT)
                }
            }
            .apply()
    }

    fun updateTokens(session: AuthSession) {
        val remember = preferences.getBoolean(KEY_REMEMBER_ME, true)
        saveSession(session, remember)
    }

    fun getSession(): AuthSession? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        return AuthSession(
            sessionId = preferences.getString(KEY_SESSION_ID, null),
            accessToken = accessToken,
            accessExpiresAtEpochMillis = preferences
                .takeIf { it.contains(KEY_ACCESS_EXPIRES_AT) }
                ?.getLong(KEY_ACCESS_EXPIRES_AT, 0L),
            refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null),
            refreshExpiresAtEpochMillis = preferences
                .takeIf { it.contains(KEY_REFRESH_EXPIRES_AT) }
                ?.getLong(KEY_REFRESH_EXPIRES_AT, 0L)
        )
    }

    fun getAccessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

    fun getCurrentUserId(): String? {
        val token = getAccessToken() ?: return null
        return try {
            val payload = token.split(".").getOrNull(1) ?: return null
            val decoded = android.util.Base64.decode(
                payload,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
            )
            val json = String(decoded)
            """"sub"\s*:\s*"([^"]+)"""".toRegex().find(json)?.groupValues?.getOrNull(1)
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_SESSION_ID)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_ACCESS_EXPIRES_AT)
            .remove(KEY_REFRESH_EXPIRES_AT)
            .remove(KEY_REMEMBER_ME)
            .apply()
    }

    fun clearIfNotRemembered() {
        if (getAccessToken().isNullOrBlank()) return
        val remember = preferences.getBoolean(KEY_REMEMBER_ME, true)
        if (!remember) clear()
    }

    companion object {
        private const val PREFERENCES_NAME = "marketplace_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
        private const val KEY_REFRESH_EXPIRES_AT = "refresh_expires_at"
        private const val KEY_REMEMBER_ME = "remember_me"
    }
}
