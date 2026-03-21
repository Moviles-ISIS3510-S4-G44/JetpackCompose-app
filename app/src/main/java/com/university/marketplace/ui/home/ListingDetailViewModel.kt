package com.university.marketplace.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val getListingByIdUseCase: GetListingByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ListingDetailUiState>(ListingDetailUiState.Loading)
    val uiState: StateFlow<ListingDetailUiState> = _uiState.asStateFlow()

    fun loadListing(id: String) {
        viewModelScope.launch {
            _uiState.value = ListingDetailUiState.Loading
            try {
                val listing = getListingByIdUseCase(id)
                _uiState.value = ListingDetailUiState.Success(listing.toUiModel())
            } catch (e: Exception) {
                _uiState.value = ListingDetailUiState.Error(e.message ?: "Failed to load listing")
            }
        }
    }
}
