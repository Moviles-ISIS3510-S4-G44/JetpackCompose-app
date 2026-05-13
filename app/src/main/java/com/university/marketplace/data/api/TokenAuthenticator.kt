package com.university.marketplace.data.api

import android.util.Log
import com.university.marketplace.data.auth.AuthApiService
import com.university.marketplace.data.auth.AuthSession
import com.university.marketplace.data.auth.AuthSessionStorage
import com.university.marketplace.data.auth.RefreshTokenRequestDto
import com.university.marketplace.data.auth.TokenResponseDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

internal class TokenAuthenticator(
    private val sessionStorage: AuthSessionStorage,
    private val refreshApi: AuthApiService
) : Authenticator {
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (AuthInterceptor.isAuthEndpoint(response.request.url.encodedPath)) {
            return null
        }

        if (responseCount(response) >= MAX_RETRIES) {
            return null
        }

        val tokenOnRequest = response.request
            .header(AuthInterceptor.AUTHORIZATION)
            ?.removePrefix("Bearer ")

        val newToken = synchronized(refreshLock) {
            val currentStoredToken = sessionStorage.getAccessToken()
            if (!currentStoredToken.isNullOrBlank() && currentStoredToken != tokenOnRequest) {
                return@synchronized currentStoredToken
            }

            val refreshToken = sessionStorage.getSession()?.refreshToken
            if (refreshToken.isNullOrBlank()) {
                sessionStorage.clear()
                return@synchronized null
            }

            try {
                val refreshResponse = runBlocking {
                    refreshApi.refresh(RefreshTokenRequestDto(refreshToken = refreshToken))
                }
                if (!refreshResponse.isSuccessful) {
                    Log.w(
                        TAG,
                        "Refresh failed with HTTP ${refreshResponse.code()}; clearing session."
                    )
                    sessionStorage.clear()
                    return@synchronized null
                }
                val body = refreshResponse.body() ?: run {
                    sessionStorage.clear()
                    return@synchronized null
                }
                val newSession = body.toAuthSession()
                sessionStorage.updateTokens(newSession)
                newSession.accessToken
            } catch (error: Throwable) {
                Log.w(TAG, "Refresh request threw; clearing session.", error)
                sessionStorage.clear()
                null
            }
        } ?: return null

        return response.request.newBuilder()
            .header(AuthInterceptor.AUTHORIZATION, "Bearer $newToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var current: Response? = response.priorResponse
        var count = 1
        while (current != null) {
            count++
            current = current.priorResponse
        }
        return count
    }

    private fun TokenResponseDto.toAuthSession(): AuthSession {
        val now = System.currentTimeMillis()
        return AuthSession(
            sessionId = sessionId,
            accessToken = accessToken,
            accessExpiresAtEpochMillis = expiresIn?.let { now + it * 1000L },
            refreshToken = refreshToken,
            refreshExpiresAtEpochMillis = refreshExpiresIn?.let { now + it * 1000L }
        )
    }

    private companion object {
        const val TAG = "TokenAuthenticator"
        const val MAX_RETRIES = 2
    }
}
