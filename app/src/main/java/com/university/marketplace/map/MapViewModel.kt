package com.university.marketplace.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.ListingsRepository
import com.university.marketplace.data.api.ListingDto
import com.university.marketplace.ui.home.ListingUiModel
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
    private val repository: ListingsRepository = ListingsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun loadListing(id: String) {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            try {
                val dto = repository.getListingById(id)
                _uiState.value = MapUiState.Success(dto.toUiModel())
            } catch (e: Exception) {
                _uiState.value = MapUiState.Error(e.message ?: "Failed to load listing")
            }
        }
    }

    private fun ListingDto.toUiModel() = ListingUiModel(
        id = id,
        name = title,
        price = price,
        imageUrl = images.firstOrNull() ?: "",
        description = description,
        category = "Category",
        condition = condition,
        latitude = try { location.split(",")[0].toDouble() } catch (e: Exception) { 4.601 },
        longitude = try { location.split(",")[1].toDouble() } catch (e: Exception) { -74.065 }
    )
}
