package com.university.marketplace.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.domain.usecase.GetListingByIdUseCase
import com.university.marketplace.ui.home.ListingUiModel
import com.university.marketplace.ui.home.toUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MapUiState {
    object Loading : MapUiState
    data class Success(val listing: ListingUiModel) : MapUiState
    data class Error(val message: String) : MapUiState
}

class MapViewModel(
    private val getListingByIdUseCase: GetListingByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun loadListing(id: String) {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            try {
                val listing = getListingByIdUseCase(id)
                _uiState.value = MapUiState.Success(listing.toUiModel())
            } catch (e: Exception) {
                _uiState.value = MapUiState.Error(e.message ?: "Failed to load listing")
            }
        }
    }
}
