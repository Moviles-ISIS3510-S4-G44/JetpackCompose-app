package com.university.marketplace.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.auth.AuthRepository
import com.university.marketplace.domain.AuthenticatedUser
import com.university.marketplace.ui.common.UserMessageMapper
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
    private val repository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String, persistSession: Boolean) {
        submitAuthRequest {
            repository.login(
                email = email,
                password = password,
                persistSession = persistSession
            )
        }
    }

    fun signUp(name: String, email: String, password: String, persistSession: Boolean) {
        submitAuthRequest {
            repository.signup(
                name = name,
                email = email,
                password = password,
                persistSession = persistSession
            )
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

    private fun submitAuthRequest(action: suspend () -> AuthenticatedUser) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null,
                    authenticatedUser = null
                )
            }

            try {
                val user = action()
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
                        errorMessage = UserMessageMapper.fromError(
                            throwable = throwable,
                            fallback = "No fue posible conectarse al servidor. Intenta nuevamente."
                        )
                    )
                }
            }
        }
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
