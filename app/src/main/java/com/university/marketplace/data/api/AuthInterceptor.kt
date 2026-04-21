package com.university.marketplace.data.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the current access token as a Bearer Authorization header to every
 * request, except for auth endpoints (login/signup/refresh/logout) and requests
 * that already carry an explicit Authorization header.
 */
internal class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (originalRequest.header(AUTHORIZATION) != null) {
            return chain.proceed(originalRequest)
        }

        if (isAuthEndpoint(originalRequest.url.encodedPath)) {
            return chain.proceed(originalRequest)
        }

        val token = tokenProvider()?.takeIf { it.isNotBlank() }
            ?: return chain.proceed(originalRequest)

        val requestWithAuth = originalRequest.newBuilder()
            .header(AUTHORIZATION, "Bearer $token")
            .build()
        return chain.proceed(requestWithAuth)
    }

    companion object {
        internal const val AUTHORIZATION = "Authorization"

        internal fun isAuthEndpoint(path: String): Boolean {
            val normalized = path.trimStart('/')
            return normalized.startsWith("auth/")
        }
    }
}
