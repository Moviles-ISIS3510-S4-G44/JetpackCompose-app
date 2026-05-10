package com.university.marketplace.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.location.LocationRepository
import com.university.marketplace.domain.FavoriteRepository
import com.university.marketplace.domain.ListingRepository
import com.university.marketplace.ui.home.ListingUiModel
import com.university.marketplace.ui.home.toUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.location.Location
import java.util.Locale

data class FavoritesUiState(
    val favoriteListings: List<ListingUiModel> = emptyList(),
    val recommendations: List<ListingUiModel> = emptyList(),
    val isLoading: Boolean = false
)

class FavoritesViewModel(
    private val favoriteRepository: FavoriteRepository,
    private val listingRepository: ListingRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private var userLocation: Location? = null

    init {
        refreshUserLocation()
        observeFavorites()
    }

    private fun refreshUserLocation() {
        viewModelScope.launch {
            locationRepository.getLastKnownLocation()?.let { loc ->
                userLocation = Location("user").apply {
                    latitude = loc.latitude
                    longitude = loc.longitude
                }
                // Refresh distances
                observeFavorites()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            favoriteRepository.getFavoriteListingIds()
                .flatMapLatest { ids ->
                    flow {
                        val listings = ids.mapNotNull { id ->
                            try {
                                listingRepository.getListingById(id)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        emit(listings)
                    }
                }
                .collect { listings ->
                    _uiState.update { 
                        it.copy(
                            favoriteListings = listings.map { l -> l.toUiModel(userLocation) },
                            isLoading = false
                        )
                    }
                    loadRecommendations()
                }
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            val recs = favoriteRepository.getRecommendations()
            _uiState.update { it.copy(recommendations = recs.map { l -> l.toUiModel(userLocation) }) }
        }
    }

    fun toggleFavorite(listingId: String) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(listingId)
        }
    }
}
