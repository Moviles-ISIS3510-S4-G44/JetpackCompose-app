package com.university.marketplace.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.location.LocationRepository
import com.university.marketplace.data.toUserFriendlyMessage
import com.university.marketplace.domain.Category
import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.CategoryRepository
import com.university.marketplace.domain.usecase.GetActiveListingsUseCase
import com.university.marketplace.domain.usecase.GetFilteredListingsUseCase
import com.university.marketplace.domain.usecase.SearchListingsByRelevanceUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max

class HomeViewModel(
    private val getActiveListingsUseCase: GetActiveListingsUseCase,
    private val searchListingsByRelevanceUseCase: SearchListingsByRelevanceUseCase,
    @Suppress("UNUSED_PARAMETER")
    private val getFilteredListingsUseCase: GetFilteredListingsUseCase,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private var allListings: List<Listing> = emptyList()
    private var currentSearchResults: List<Listing>? = null
    private var searchJob: Job? = null
    private var userLocation: Location? = null

    private val listingInterestWeights = mutableMapOf<String, Float>()
    private val categoryInterestWeights = mutableMapOf<String, Float>()

    init {
        loadCategories()
        observeListings()
        observeSearch()
        loadListings()
        refreshUserLocation()
    }

    fun refreshUserLocation() {
        viewModelScope.launch {
            locationRepository.getLastKnownLocation()?.let { loc ->
                userLocation = Location("user").apply {
                    latitude = loc.latitude
                    longitude = loc.longitude
                }
                applyFiltersAndPublish()
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            runCatching { categoryRepository.getCategories() }
                .onSuccess { _categories.value = it }
        }
    }

    private fun observeListings() {
        viewModelScope.launch {
            getActiveListingsUseCase().collectLatest { listings ->
                allListings = listings
                if (_searchQuery.value.isEmpty()) {
                    currentSearchResults = null
                }
                applyFiltersAndPublish()
            }
        }
    }

    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        currentSearchResults = null
                        applyFiltersAndPublish()
                    } else {
                        executeSearch(query)
                    }
                }
        }
    }

    private fun executeSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            searchListingsByRelevanceUseCase.execute(query).collectLatest { results ->
                currentSearchResults = results
                applyFiltersAndPublish()
            }
        }
    }

    fun loadListings() {
        viewModelScope.launch {
            runCatching { getActiveListingsUseCase.refresh() }
                .onFailure {
                    if (allListings.isEmpty()) {
                        _uiState.value = HomeUiState.Error(
                            it.toUserFriendlyMessage(fallback = "No se pudieron cargar las publicaciones")
                        )
                    }
                }
        }
    }

    fun showOfflineState() {
        if (_uiState.value is HomeUiState.Success) return
        _uiState.value = HomeUiState.Error(
            "Parece que no tienes conexion. Revisa tu internet e intenta de nuevo."
        )
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(categoryId: String?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
        applyFiltersAndPublish()
    }

    fun onListingOpened(listing: ListingUiModel) {
        listingInterestWeights[listing.id] = (listingInterestWeights[listing.id] ?: 0f) + 1.5f
        val normalizedCategory = listing.category.trim().lowercase()
        categoryInterestWeights[normalizedCategory] =
            (categoryInterestWeights[normalizedCategory] ?: 0f) + 0.7f
    }

    private fun applyFiltersAndPublish() {
        val baseListings = currentSearchResults ?: allListings
        val categoryId = _selectedCategoryId.value

        val filtered = baseListings
            .asSequence()
            .filter { listing ->
                categoryId == null || listing.categoryId.equals(categoryId, ignoreCase = true)
            }
            .toList()

        val weighted = applyUserBehaviorWeights(filtered)
        updateSections(weighted.map { it.toUiModelWithDistance() })
    }

    private fun Listing.toUiModelWithDistance(): ListingUiModel {
        val uiModel = toUiModel()
        val distanceStr = if (latitude != null && longitude != null && userLocation != null) {
            val dest = Location("dest").apply {
                latitude = this@toUiModelWithDistance.latitude
                longitude = this@toUiModelWithDistance.longitude
            }
            val distanceMeters = userLocation!!.distanceTo(dest)
            if (distanceMeters < 1000) {
                "Aprox. ${distanceMeters.toInt()} m"
            } else {
                String.format(Locale.US, "Aprox. %.1f km", distanceMeters / 1000f)
            }
        } else {
            null
        }
        return uiModel.copy(distance = distanceStr)
    }

    private fun applyUserBehaviorWeights(listings: List<Listing>): List<Listing> {
        if (listings.isEmpty()) return emptyList()
        return listings.sortedByDescending { listing ->
            val byListing = (listingInterestWeights[listing.id] ?: 0f).toDouble()
            val byCategory = (categoryInterestWeights[listing.categoryId.trim().lowercase()] ?: 0f).toDouble()
            val priceBoost = max(0.0, 1.0 - (listing.price / 10000000.0))
            byListing + byCategory + (priceBoost * 0.05)
        }
    }

    private fun updateSections(listings: List<ListingUiModel>) {
        val isFiltering = _searchQuery.value.isNotEmpty() || _selectedCategoryId.value != null
        val featured = listings.take(4)
        val recent = listings.drop(4)
        _uiState.value = HomeUiState.Success(
            featured = featured,
            recent = recent,
            isSearching = isFiltering
        )
    }
}
