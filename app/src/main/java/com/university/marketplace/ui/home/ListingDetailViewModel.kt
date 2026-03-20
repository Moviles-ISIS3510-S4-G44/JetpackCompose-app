package com.university.marketplace.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.ListingsRepository
import com.university.marketplace.data.api.ListingDto
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
    private val repository: ListingsRepository = ListingsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ListingDetailUiState>(ListingDetailUiState.Loading)
    val uiState: StateFlow<ListingDetailUiState> = _uiState.asStateFlow()

    fun loadListing(id: String) {
        viewModelScope.launch {
            _uiState.value = ListingDetailUiState.Loading
            try {
                val dto = repository.getListingById(id)
                _uiState.value = ListingDetailUiState.Success(dto.toUiModel())
            } catch (e: Exception) {
                _uiState.value = ListingDetailUiState.Error(e.message ?: "Failed to load listing")
            }
        }
    }

    private fun ListingDto.toUiModel() = ListingUiModel(
        id = id,
        name = title,
        price = price,
        imageUrl = images.firstOrNull() ?: "",
        description = description,
        category = "Category", // DTO has category_id, mapping to string for UI
        condition = condition,
        // Since backend doesn't have lat/lng in contract, using defaults or parsing location string
        latitude = try { location.split(",")[0].toDouble() } catch (e: Exception) { 4.601 },
        longitude = try { location.split(",")[1].toDouble() } catch (e: Exception) { -74.065 }
    )
}
