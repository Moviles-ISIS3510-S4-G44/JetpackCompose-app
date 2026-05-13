package com.university.marketplace.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.location.LocationRepository
import com.university.marketplace.data.toUserFriendlyMessage
import com.university.marketplace.domain.usecase.GetListingByIdUseCase
import com.university.marketplace.ui.home.toUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class MapViewModel(
    private val getListingByIdUseCase: GetListingByIdUseCase,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun loadListing(id: String) {
        refreshUserLocation()
        viewModelScope.launch {
            _uiState.update { it.copy(content = MapUiState.Content.Loading) }
            try {
                val listing = getListingByIdUseCase(id)
                _uiState.update { it.copy(content = MapUiState.Content.Success(listing.toUiModel())) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(content = MapUiState.Content.Error(
                        e.toUserFriendlyMessage(fallback = "Failed to load listing")
                    ))
                }
            }
        }
    }

    fun refreshUserLocation() {
        viewModelScope.launch {
            locationRepository.getLastKnownLocation()?.let { loc ->
                _uiState.update { it.copy(userLocation = loc.latitude to loc.longitude) }
            }
        }
    }

    fun showOfflineState() {
        if (_uiState.value.content is MapUiState.Content.Success) return
        _uiState.update { 
            it.copy(content = MapUiState.Content.Error(
                "You appear to be offline. Please check your connection and try again."
            ))
        }
    }

    fun resetToLoading() {
        if (_uiState.value.content is MapUiState.Content.Success) return
        _uiState.update { it.copy(content = MapUiState.Content.Loading) }
    }
}
