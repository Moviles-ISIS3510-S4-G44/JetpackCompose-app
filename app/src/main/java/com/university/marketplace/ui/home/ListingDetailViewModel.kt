package com.university.marketplace.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.InteractionsRepository
import com.university.marketplace.data.location.LocationRepository
import com.university.marketplace.data.toUserFriendlyMessage
import com.university.marketplace.domain.FavoriteRepository
import com.university.marketplace.domain.usecase.CreatePurchaseUseCase
import com.university.marketplace.domain.usecase.GetListingByIdUseCase
import com.university.marketplace.ui.common.toUserFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListingDetailUiState(
    val content: Content = Content.Loading,
    val isFavorited: Boolean = false,
    val userLocation: Pair<Double, Double>? = null
) {
    sealed interface Content {
        object Loading : Content
        data class Success(val listing: ListingUiModel) : Content
        data class Error(val message: String) : Content
    }
}

sealed interface PurchaseUiState {
    data object Idle : PurchaseUiState
    data object Loading : PurchaseUiState
    data object Success : PurchaseUiState
    data class Error(val message: String) : PurchaseUiState
}

class ListingDetailViewModel(
    private val getListingByIdUseCase: GetListingByIdUseCase,
    private val interactionsRepository: InteractionsRepository,
    private val favoriteRepository: FavoriteRepository,
    private val locationRepository: LocationRepository,
    private val createPurchaseUseCase: CreatePurchaseUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListingDetailUiState())
    val uiState: StateFlow<ListingDetailUiState> = _uiState.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseUiState>(PurchaseUiState.Idle)
    val purchaseState: StateFlow<PurchaseUiState> = _purchaseState.asStateFlow()

    fun loadListing(id: String) {
        refreshUserLocation()
        favoriteRepository.isFavorite(id)
            .onEach { isFav ->
                _uiState.update { it.copy(isFavorited = isFav) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            _uiState.update { it.copy(content = ListingDetailUiState.Content.Loading) }
            try {
                val listing = getListingByIdUseCase(id)
                val userLoc = _uiState.value.userLocation?.let { (lat, lon) ->
                    android.location.Location("user").apply {
                        latitude = lat
                        longitude = lon
                    }
                }
                _uiState.update { it.copy(content = ListingDetailUiState.Content.Success(listing.toUiModel(userLoc))) }
                viewModelScope.launch {
                    interactionsRepository.registerVisit(listing.id)
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(content = ListingDetailUiState.Content.Error(
                        e.toUserFriendlyMessage(fallback = "Failed to load listing")
                    ))
                }
            }
        }
    }

    fun refreshUserLocation() {
        viewModelScope.launch {
            locationRepository.getLastKnownLocation()?.let { loc ->
                val newLoc = loc.latitude to loc.longitude
                _uiState.update { state ->
                    val updatedContent = if (state.content is ListingDetailUiState.Content.Success) {
                        val androidLoc = android.location.Location("user").apply {
                            latitude = loc.latitude
                            longitude = loc.longitude
                        }
                        // We need the original domain Listing here, but we only have ListingUiModel
                        // This is a bit tricky. Maybe we should store the domain listing too?
                        // Or just update the distance in the UI model.
                        val currentListing = state.content.listing
                        // We can't easily re-calculate from UiModel because it doesn't have all data.
                        // Actually, ListingUiModel HAS latitude and longitude!
                        
                        val baseLocation = currentListing.locationName
                        val dest = android.location.Location("dest").apply {
                            latitude = currentListing.latitude ?: 0.0
                            longitude = currentListing.longitude ?: 0.0
                        }
                        val distanceStr = if (currentListing.latitude != null && currentListing.longitude != null) {
                            val distanceMeters = androidLoc.distanceTo(dest)
                            if (distanceMeters < 1000) {
                                "$baseLocation • ${distanceMeters.toInt()}m"
                            } else {
                                java.util.Locale.US.let { locale ->
                                    String.format(locale, "%s • %.1f km", baseLocation, distanceMeters / 1000f)
                                }
                            }
                        } else {
                            baseLocation
                        }
                        ListingDetailUiState.Content.Success(currentListing.copy(distance = distanceStr))
                    } else {
                        state.content
                    }
                    state.copy(userLocation = newLoc, content = updatedContent)
                }
            }
        }
    }

    fun showOfflineState() {
        if (_uiState.value.content is ListingDetailUiState.Content.Success) return
        _uiState.update { 
            it.copy(content = ListingDetailUiState.Content.Error(
                "You appear to be offline. Please check your connection and try again."
            ))
        }
    }

    fun resetToLoading() {
        if (_uiState.value.content is ListingDetailUiState.Content.Success) return
        _uiState.update { it.copy(content = ListingDetailUiState.Content.Loading) }
    }

    fun purchase(listingId: String) {
        val useCase = createPurchaseUseCase ?: return
        viewModelScope.launch {
            _purchaseState.value = PurchaseUiState.Loading
            try {
                useCase(listingId)
                _purchaseState.value = PurchaseUiState.Success
            } catch (e: Exception) {
                _purchaseState.value = PurchaseUiState.Error(
                    e.toUserFriendlyMessage(fallback = "Purchase failed")
                )
            }
        }
    }

    fun toggleFavorite(listingId: String) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(listingId)
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseUiState.Idle
    }
}
