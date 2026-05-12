package com.university.marketplace.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.auth.AuthException
import com.university.marketplace.data.auth.AuthRepository
import com.university.marketplace.data.location.LocationRepository
import com.university.marketplace.data.isNetworkConnectivityError
import com.university.marketplace.domain.AuthenticatedUser
import com.university.marketplace.ui.common.toUserFriendlyMessage
import com.university.marketplace.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isInvalidCredentials: Boolean = false,
    val authenticatedUser: AuthenticatedUser? = null
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String, persistSession: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isInvalidCredentials = false, authenticatedUser = null) }

            try {
                val (user, location) = supervisorScope {
                    val authDeferred = async(Dispatchers.IO) {
                        repository.login(email, password, persistSession)
                    }

                    val locationDeferred = async(Dispatchers.IO) {
                        locationRepository.getLastKnownLocation()
                    }

                    authDeferred.await() to locationDeferred.await()
                }

                Log.d("AuthStrategy", "Login: ${user.email} | Ubicación: $location")

                _uiState.update { it.copy(isLoading = false, authenticatedUser = user) }
            } catch (throwable: Throwable) {
                val message = throwable.toUserMessage()
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = message,
                        isInvalidCredentials = message == INVALID_CREDENTIALS_ERROR
                    ) 
                }
            }
        }
    }

    fun signUp(name: String, email: String, password: String, persistSession: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isInvalidCredentials = false, authenticatedUser = null) }

            try {
                val (user, location) = supervisorScope {
                    val authDeferred = async(Dispatchers.IO) {
                        repository.signup(name, email, password, persistSession)
                    }

                    val locationDeferred = async(Dispatchers.IO) {
                        locationRepository.getLastKnownLocation()
                    }

                    authDeferred.await() to locationDeferred.await()
                }

                Log.d("AuthStrategy", "Registro: ${user.email} | Ubicación: $location")

                _uiState.update { it.copy(isLoading = false, authenticatedUser = user) }
            } catch (throwable: Throwable) {
                val message = throwable.toUserMessage()
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = message,
                        isInvalidCredentials = message == INVALID_CREDENTIALS_ERROR
                    ) 
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, isInvalidCredentials = false) }
    }

    fun consumeAuthentication() {
        _uiState.update { it.copy(authenticatedUser = null) }
    }

    private fun Throwable.toUserMessage(): String {
        if (isNetworkConnectivityError()) {
            return "You appear to be offline. Please check your connection and try again."
        }
        val fallback = "We could not connect to the server. Check the API base URL and try again."
        return when (this) {
            is AuthException -> message.toFriendlyAuthMessage()
            else -> toUserFriendlyMessage()
        }
    }

    private fun String?.toFriendlyAuthMessage(): String {
        val value = this.orEmpty().lowercase()
        return when {
            value.contains("invalid") || 
            value.contains("credentials") || 
            value.contains("incorrect") || 
            value.contains("password") || 
            value.contains("user not found") ||
            value.contains("unauthorized") -> INVALID_CREDENTIALS_ERROR
            value.contains("already registered") || value.contains("409") -> "Este correo ya tiene una cuenta registrada."
            value.contains("expired") || value.contains("session") -> "Tu sesión expiró. Inicia sesión nuevamente."
            else -> "No pudimos completar la solicitud. Intenta nuevamente."
            value.contains("invalid credentials") -> "Correo o contraseña incorrectos."
            value.contains("already registered") -> "Este correo ya tiene una cuenta registrada."
            value.contains("session has expired") -> "Tu sesión expiró. Inicia sesión nuevamente."
            else -> if (BuildConfig.DEBUG && !this.isNullOrBlank()) {
                this
            } else {
                "No pudimos completar la solicitud. Intenta nuevamente."
            }
        }
    }

    companion object {
        const val INVALID_CREDENTIALS_ERROR = "Correo o contraseña incorrectos."
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository,
    private val locationRepository: LocationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository, locationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
