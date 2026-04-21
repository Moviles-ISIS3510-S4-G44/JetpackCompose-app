package com.university.marketplace.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.InteractionsRepository
import com.university.marketplace.data.toUserFriendlyMessage
import com.university.marketplace.domain.usecase.GetListingByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ListingDetailUiState {
    object Loading : ListingDetailUiState
    data class Success(val listing: ListingUiModel) : ListingDetailUiState
    data class Error(val message: String) : ListingDetailUiState
}

class ListingDetailViewModel(
    private val getListingByIdUseCase: GetListingByIdUseCase,
    private val interactionsRepository: InteractionsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ListingDetailUiState>(ListingDetailUiState.Loading)
    val uiState: StateFlow<ListingDetailUiState> = _uiState.asStateFlow()

    fun loadListing(id: String) {
        viewModelScope.launch {
            _uiState.value = ListingDetailUiState.Loading
            try {
                val listing = getListingByIdUseCase(id)
                _uiState.value = ListingDetailUiState.Success(listing.toUiModel())
                viewModelScope.launch {
                    interactionsRepository.registerVisit(listing.id)
                }
            } catch (e: Exception) {
                _uiState.value = ListingDetailUiState.Error(
                    e.toUserFriendlyMessage(fallback = "Failed to load listing")
                )
            }
        }
    }

    fun showOfflineState() {
        if (_uiState.value is ListingDetailUiState.Success) return
        _uiState.value = ListingDetailUiState.Error(
            "You appear to be offline. Please check your connection and try again."
        )
    }

    fun resetToLoading() {
        if (_uiState.value is ListingDetailUiState.Success) return
        _uiState.value = ListingDetailUiState.Loading
    }
}
