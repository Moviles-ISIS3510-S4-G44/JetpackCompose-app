package com.university.marketplace.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.toUserFriendlyMessage
import com.university.marketplace.domain.Category
import com.university.marketplace.domain.CategoryRepository
import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.usecase.GetActiveListingsUseCase
import com.university.marketplace.domain.usecase.GetFilteredListingsUseCase
import com.university.marketplace.domain.usecase.SearchListingsByRelevanceUseCase
import com.university.marketplace.data.location.LocationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

class HomeViewModel(
    private val getActiveListingsUseCase: GetActiveListingsUseCase,
    private val searchListingsByRelevanceUseCase: SearchListingsByRelevanceUseCase,
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
    private var searchJob: Job? = null
    private var userLocation: Location? = null

    init {
        loadCategories()
        observeListings()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            runCatching { _categories.value = categoryRepository.getCategories() }
        }
    }

    fun refreshUserLocation() {
        fetchUserLocation()
    }

    private fun fetchUserLocation() {
        viewModelScope.launch {
            locationRepository.getLastKnownLocation()?.let {
                userLocation = Location("gps").apply {
                    latitude = it.latitude
                    longitude = it.longitude
                }
                applyFiltersAndPublish()
            }
        }
    }

    private fun observeListings() {
        viewModelScope.launch {
            getActiveListingsUseCase().collectLatest { listings ->
                allListings = listings
                applyFiltersAndPublish()
            }
        }
    }

    fun loadListings() {
        viewModelScope.launch {
            getActiveListingsUseCase.refresh()
        }
    }

    fun showOfflineState() {
        if (_uiState.value is HomeUiState.Success) return
        _uiState.value = HomeUiState.Error(
            "You appear to be offline. Please check your connection and try again."
        )
    }

    private fun applyFiltersAndPublish() {
        val filtered = allListings.filter { listing ->
            val matchesCategory = _selectedCategoryId.value == null || listing.categoryId == _selectedCategoryId.value
            matchesCategory
        }
        updateSections(filtered.map { it.toUiModel() })
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

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            if (query.isNotEmpty()) {
                searchListingsByRelevanceUseCase.execute(query).collectLatest { results ->
                    updateSections(results.map { it.toUiModel() })
                }
            } else {
                applyFiltersAndPublish()
            }
        }
    }

    fun onCategorySelected(categoryId: String?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
        applyFiltersAndPublish()
    }
}
