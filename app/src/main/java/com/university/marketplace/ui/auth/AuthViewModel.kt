package com.university.marketplace.ui.auth

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
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null,
                    authenticatedUser = null
                )
            }

            try {
                // Iniciamos ambas tareas en paralelo usando async en Dispatchers.IO
                val authDeferred = async(Dispatchers.IO) {
                    repository.login(
                        email = email,
                        password = password,
                        persistSession = persistSession
                    )
                }

                val locationDeferred = async(Dispatchers.IO) {
                    locationRepository.getLastKnownLocation()
                }

                val user = authDeferred.await()
                val location = locationDeferred.await()



                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        authenticatedUser = user,
                        errorMessage = null
                    )
                }
            } catch (throwable: Throwable) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        authenticatedUser = null,
                        errorMessage = throwable.toUserMessage()
                    )
                }
            }
        }
    }

    fun signUp(name: String, email: String, password: String, persistSession: Boolean) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null,
                    authenticatedUser = null
                )
            }

            try {

                val authDeferred = async(Dispatchers.IO) {
                    repository.signup(
                        name = name,
                        email = email,
                        password = password,
                        persistSession = persistSession
                    )
                }

                val locationDeferred = async(Dispatchers.IO) {
                    locationRepository.getLastKnownLocation()
                }

                val user = authDeferred.await()
                val location = locationDeferred.await()

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        authenticatedUser = user,
                        errorMessage = null
                    )
                }
            } catch (throwable: Throwable) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        authenticatedUser = null,
                        errorMessage = throwable.toUserMessage()
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { currentState ->
            currentState.copy(errorMessage = null)
        }
    }

    fun consumeAuthentication() {
        _uiState.update { currentState ->
            currentState.copy(authenticatedUser = null)
        }
    }

    private fun Throwable.toUserMessage(): String {
        val fallback = "We could not connect to the server. Check the API base URL and try again."
        return when (this) {
            is AuthException -> message ?: "We could not complete the request."
            else -> {
                if (BuildConfig.DEBUG) {
                    val detail = message ?: "No additional detail."
                    "$fallback\n${this::class.simpleName}: $detail\nBase URL: ${BuildConfig.API_BASE_URL}"
                } else {
                    fallback
                }
            }
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
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
