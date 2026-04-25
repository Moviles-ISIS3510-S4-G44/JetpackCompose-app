package com.university.marketplace.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.auth.AuthException
import com.university.marketplace.data.auth.AuthRepository
import com.university.marketplace.data.isNetworkConnectivityError
import com.university.marketplace.data.location.LocationRepository
import com.university.marketplace.domain.AuthenticatedUser
import com.university.marketplace.ui.common.toUserFriendlyMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null, authenticatedUser = null) }

            try {

                val authDeferred = async(Dispatchers.IO) {
                    repository.login(email, password, persistSession)
                }

                val locationDeferred = async(Dispatchers.IO) {
                    locationRepository.getLastKnownLocation()
                }


                val user = authDeferred.await()
                val location = locationDeferred.await()


                Log.d("AuthStrategy", "Login: ${user.email} en ubicación: $location")

                _uiState.update { it.copy(isLoading = false, authenticatedUser = user) }
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.toUserMessage()) }
            }
        }
    }

    fun signUp(name: String, email: String, password: String, persistSession: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, authenticatedUser = null) }

            try {

                val authDeferred = async(Dispatchers.IO) {
                    repository.signup(name, email, password, persistSession)
                }

                val locationDeferred = async(Dispatchers.IO) {
                    locationRepository.getLastKnownLocation()
                }

                val user = authDeferred.await()
                val location = locationDeferred.await()

                Log.d("AuthStrategy", "Registro: ${user.email} desde: $location")

                _uiState.update { it.copy(isLoading = false, authenticatedUser = user) }
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.toUserMessage()) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
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
            value.contains("invalid credentials") -> "Correo o contrasena incorrectos. Revisa tus datos e intenta de nuevo."
            value.contains("already registered") -> "Este correo ya tiene una cuenta registrada."
            value.contains("session has expired") -> "Tu sesion termino. Inicia sesion de nuevo para continuar."
            else -> "No pudimos completar la solicitud. Intenta nuevamente."
        }
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
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
