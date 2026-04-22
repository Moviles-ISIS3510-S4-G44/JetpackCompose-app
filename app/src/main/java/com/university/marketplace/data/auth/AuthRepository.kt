package com.university.marketplace.data.auth

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.domain.AuthenticatedUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AuthRepository {
    suspend fun login(email: String, password: String, persistSession: Boolean): AuthenticatedUser
    suspend fun signup(name: String, email: String, password: String, persistSession: Boolean): AuthenticatedUser
    suspend fun getCurrentUser(): AuthenticatedUser
    suspend fun logout()
    suspend fun refreshAccessToken(): String
    fun hasActiveSession(): Boolean
    fun clearSession()
}

class DefaultAuthRepository(
    private val apiService: AuthApiService,
    private val sessionStorage: AuthSessionStorage,
    private val gson: Gson = Gson()
) : AuthRepository {
    override suspend fun login(
        email: String,
        password: String,
        persistSession: Boolean
    ): AuthenticatedUser = withContext(Dispatchers.IO) {
        val loginResponse = apiService.login(email = email, password = password)
        if (!loginResponse.isSuccessful) {
            throw buildAuthException(
                statusCode = loginResponse.code(),
                errorBody = loginResponse.errorBody()?.string(),
                unauthorizedMessage = "Invalid credentials. Please check your email and password."
            )
        }

        val body = loginResponse.body()
            ?: throw AuthException("The server did not return an access token.")

        val session = body.toAuthSession()
        sessionStorage.saveSession(session, rememberAcrossRestarts = persistSession)

        try {
            fetchCurrentUser()
        } catch (error: Throwable) {
            sessionStorage.clear()
            throw error
        }
    }

    override suspend fun signup(
        name: String,
        email: String,
        password: String,
        persistSession: Boolean
    ): AuthenticatedUser = withContext(Dispatchers.IO) {
        val signupResponse = apiService.signup(
            SignupRequestDto(
                name = name,
                email = email,
                password = password
            )
        )
        if (!signupResponse.isSuccessful) {
            throw buildAuthException(
                statusCode = signupResponse.code(),
                errorBody = signupResponse.errorBody()?.string()
            )
        }

        login(email = email, password = password, persistSession = persistSession)
    }

    override suspend fun getCurrentUser(): AuthenticatedUser = withContext(Dispatchers.IO) {
        if (sessionStorage.getAccessToken().isNullOrBlank()) {
            throw UnauthorizedAuthException("No active session was found.")
        }
        try {
            fetchCurrentUser()
        } catch (unauthorized: UnauthorizedAuthException) {
            sessionStorage.clear()
            throw unauthorized
        }
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        val session = sessionStorage.getSession()
        if (session?.sessionId != null && !session.refreshToken.isNullOrBlank()) {
            try {
                val response = apiService.logout(
                    LogoutRequestDto(
                        sessionId = session.sessionId,
                        refreshToken = session.refreshToken
                    )
                )
                if (!response.isSuccessful) {
                    Log.w(
                        TAG,
                        "Server logout failed with ${response.code()}; clearing local session anyway."
                    )
                }
            } catch (error: Throwable) {
                Log.w(TAG, "Server logout request failed; clearing local session anyway.", error)
            }
        }
        sessionStorage.clear()
    }

    override suspend fun refreshAccessToken(): String = withContext(Dispatchers.IO) {
        val refreshToken = sessionStorage.getSession()?.refreshToken
        if (refreshToken.isNullOrBlank()) {
            sessionStorage.clear()
            throw UnauthorizedAuthException("Your session has expired. Please sign in again.")
        }

        val response = try {
            apiService.refresh(RefreshTokenRequestDto(refreshToken = refreshToken))
        } catch (error: Throwable) {
            sessionStorage.clear()
            throw error
        }

        if (!response.isSuccessful) {
            val exception = buildAuthException(
                statusCode = response.code(),
                errorBody = response.errorBody()?.string(),
                unauthorizedMessage = "Your session has expired. Please sign in again."
            )
            sessionStorage.clear()
            throw exception
        }

        val body = response.body()
            ?: throw AuthException("The server did not return a refreshed access token.")

        val newSession = body.toAuthSession()
        sessionStorage.updateTokens(newSession)
        newSession.accessToken
    }

    override fun hasActiveSession(): Boolean = !sessionStorage.getAccessToken().isNullOrBlank()

    override fun clearSession() {
        sessionStorage.clear()
    }

    private suspend fun fetchCurrentUser(): AuthenticatedUser {
        val response = apiService.getCurrentUser()
        if (!response.isSuccessful) {
            throw buildAuthException(
                statusCode = response.code(),
                errorBody = response.errorBody()?.string(),
                unauthorizedMessage = "Your session has expired. Please sign in again."
            )
        }
        return response.body()?.toDomain()
            ?: throw AuthException("The server did not return user information.")
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

    private fun CurrentUserResponseDto.toDomain(): AuthenticatedUser {
        return AuthenticatedUser(
            id = id,
            name = name,
            email = email,
            rating = rating
        )
    }

    private fun buildAuthException(
        statusCode: Int,
        errorBody: String?,
        unauthorizedMessage: String = "We could not complete the request. Please try again."
    ): AuthException {
        val apiMessage = runCatching {
            gson.fromJson(errorBody, ApiErrorResponseDto::class.java)?.detail
        }.getOrNull()

        val message =
            if (!apiMessage.isNullOrBlank()) {
                apiMessage
            } else {
                when (statusCode) {
                    401 -> unauthorizedMessage
                    409 -> "This email is already registered."
                    else -> "We could not complete the request. Please try again."
                }
            }

        return if (statusCode == 401) {
            UnauthorizedAuthException(message)
        } else {
            AuthException(message)
        }
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}

open class AuthException(message: String) : Exception(message)

class UnauthorizedAuthException(message: String) : AuthException(message)

object AuthRepositoryFactory {
    fun create(context: Context): AuthRepository {
        NetworkModule.initialize(context)
        return DefaultAuthRepository(
            apiService = NetworkModule.authApi,
            sessionStorage = NetworkModule.authSessionStorage
        )
    }
}
