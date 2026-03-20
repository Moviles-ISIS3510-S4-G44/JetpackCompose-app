package com.university.marketplace.data.auth

import android.content.Context
import com.google.gson.Gson
import com.university.marketplace.domain.AuthenticatedUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AuthRepository {
    suspend fun login(email: String, password: String, persistSession: Boolean): AuthenticatedUser
    suspend fun signup(name: String, email: String, password: String, persistSession: Boolean): AuthenticatedUser
    suspend fun getCurrentUser(): AuthenticatedUser
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

        val token = loginResponse.body()?.accessToken
            ?: throw AuthException("The server did not return an access token.")

        if (persistSession) {
            sessionStorage.saveAccessToken(token)
        } else {
            sessionStorage.clear()
        }

        try {
            getCurrentUser(token)
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
        val token = sessionStorage.getAccessToken()
            ?: throw AuthException("No active session was found.")
        getCurrentUser(token)
    }

    override fun hasActiveSession(): Boolean = !sessionStorage.getAccessToken().isNullOrBlank()

    override fun clearSession() {
        sessionStorage.clear()
    }

    private suspend fun getCurrentUser(token: String): AuthenticatedUser {
        val currentUserResponse = apiService.getCurrentUser(authorization = "Bearer $token")
        if (!currentUserResponse.isSuccessful) {
            if (currentUserResponse.code() == 401) {
                sessionStorage.clear()
            }
            throw buildAuthException(
                statusCode = currentUserResponse.code(),
                errorBody = currentUserResponse.errorBody()?.string(),
                unauthorizedMessage = "Your session has expired. Please sign in again."
            )
        }

        return currentUserResponse.body()?.toDomain()
            ?: throw AuthException("The server did not return user information.")
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
}

open class AuthException(message: String) : Exception(message)

class UnauthorizedAuthException(message: String) : AuthException(message)

object AuthRepositoryFactory {
    fun create(context: Context): AuthRepository {
        return DefaultAuthRepository(
            apiService = AuthApiFactory.create(),
            sessionStorage = AuthSessionStorage(context)
        )
    }
}
