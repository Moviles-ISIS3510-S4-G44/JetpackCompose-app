package com.university.marketplace.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.ProfileVisitsRepository
import com.university.marketplace.data.auth.AuthRepository
import com.university.marketplace.domain.AuthenticatedUser
import com.university.marketplace.domain.usecase.GetListingsBySellerUseCase
import com.university.marketplace.ui.home.ListingUiModel
import com.university.marketplace.ui.home.toUiModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OtherUserProfileUiState(
    val isLoading: Boolean = false,
    val user: AuthenticatedUser? = null,
    val listings: List<ListingUiModel> = emptyList(),
    val errorMessage: String? = null
)

class OtherUserProfileViewModel(
    private val getListingsBySellerUseCase: GetListingsBySellerUseCase,
    private val profileVisitsRepository: ProfileVisitsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtherUserProfileUiState())
    val uiState: StateFlow<OtherUserProfileUiState> = _uiState.asStateFlow()

    fun loadSellerProfile(sellerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // listings fetch
                val listingsDeferred = async { getListingsBySellerUseCase(sellerId).map { it.toUiModel() } }
                
                // Authoritative user fetch with fallback to stay stable
                val user = try {
                    authRepository.getUserProfile(sellerId)
                } catch (e: Exception) {
                    null
                }

                val listings = listingsDeferred.await()
                
                _uiState.update { it.copy(isLoading = false, user = user, listings = listings) }
                
                viewModelScope.launch {
                    profileVisitsRepository.registerProfileVisit(sellerId)
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = e.message ?: "Failed to load seller profile"
                    )
                }
            }
        }
    }
}
