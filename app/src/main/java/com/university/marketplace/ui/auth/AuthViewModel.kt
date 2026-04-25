package com.university.marketplace.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.university.marketplace.BuildConfig
import com.university.marketplace.data.auth.AuthException
import com.university.marketplace.data.auth.AuthRepository
import com.university.marketplace.data.location.LocationRepository
import com.university.marketplace.domain.AuthenticatedUser
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
        val fallback = "We could not connect to the server."
        return when (this) {
            is AuthException -> message ?: "Auth Error"
            else -> if (BuildConfig.DEBUG) "$fallback\n${this.message}" else fallback
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
