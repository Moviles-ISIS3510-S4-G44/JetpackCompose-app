package com.university.marketplace.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.domain.usecase.GetListingsBySellerUseCase
import com.university.marketplace.ui.home.ListingUiModel
import com.university.marketplace.ui.home.toUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OtherUserProfileUiState(
    val isLoading: Boolean = false,
    val listings: List<ListingUiModel> = emptyList(),
    val errorMessage: String? = null
)

class OtherUserProfileViewModel(
    private val getListingsBySellerUseCase: GetListingsBySellerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtherUserProfileUiState())
    val uiState: StateFlow<OtherUserProfileUiState> = _uiState.asStateFlow()

    fun loadSellerProfile(sellerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val listings = getListingsBySellerUseCase(sellerId).map { it.toUiModel() }
                _uiState.update { it.copy(isLoading = false, listings = listings) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = e.message ?: "Failed to load seller listings"
                    )
                }
            }
        }
    }
}
