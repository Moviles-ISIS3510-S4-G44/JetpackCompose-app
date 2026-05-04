package com.university.marketplace.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.domain.FavoriteRepository
import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.ListingRepository
import com.university.marketplace.ui.home.ListingUiModel
import com.university.marketplace.ui.home.toUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favoriteListings: List<ListingUiModel> = emptyList(),
    val recommendations: List<ListingUiModel> = emptyList(),
    val isLoading: Boolean = false
)

class FavoritesViewModel(
    private val favoriteRepository: FavoriteRepository,
    private val listingRepository: ListingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        observeFavorites()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            favoriteRepository.getFavoriteListingIds()
                .flatMapLatest { ids ->
                    // Fetch full listing details for each favorite ID
                    // In a real app, we'd have a specific DAO method for this
                    flow {
                        val listings = ids.mapNotNull { id ->
                            try {
                                listingRepository.getListingById(id)
                            } catch (e: Exception) {
                                // If offline and not in cache, we might want to show something else
                                // but for now we just filter it out
                                null
                            }
                        }
                        emit(listings)
                    }
                }
                .collect { listings ->
                    _uiState.update { 
                        it.copy(
                            favoriteListings = listings.map { l -> l.toUiModel() },
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
            _uiState.update { it.copy(recommendations = recs.map { l -> l.toUiModel() }) }
        }
    }

    fun toggleFavorite(listingId: String) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(listingId)
        }
    }
}
