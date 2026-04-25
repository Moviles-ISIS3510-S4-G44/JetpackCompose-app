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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max

class HomeViewModel(
    private val getActiveListingsUseCase: GetActiveListingsUseCase,
    private val searchListingsByRelevanceUseCase: SearchListingsByRelevanceUseCase,
    private val getFilteredListingsUseCase: GetFilteredListingsUseCase,
    private val categoryRepository: CategoryRepository
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

    init {
        loadCategories()
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
                if (_searchQuery.value.isEmpty()) currentSearchResults = null
                applyFiltersAndPublish()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isNotEmpty()) {
                        executeSearch(query)
                    } else {
                        currentSearchResults = null
                        applyFiltersAndPublish()
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
            _uiState.value = HomeUiState.Loading
            try {
                allListings = getActiveListingsUseCase()
                updateSections(allListings.map { it.toUiModel() })
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    e.toUserFriendlyMessage(fallback = "An unknown error occurred")
                )
            }
        }
    }

    fun showOfflineState() {
        if (_uiState.value is HomeUiState.Success) return
        _uiState.value = HomeUiState.Error(
            "You appear to be offline. Please check your connection and try again."
        )
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
        if (query.isEmpty() && _selectedCategoryId.value == null) {
            updateSections(allListings.map { it.toUiModel() })
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            fetchFiltered()
        }
    }

    fun onCategorySelected(categoryId: String?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            fetchFiltered()
        }
    }

    private suspend fun fetchFiltered() {
        val q = _searchQuery.value.takeIf { it.isNotEmpty() }
        val catId = _selectedCategoryId.value
        if (q == null && catId == null) {
            updateSections(allListings.map { it.toUiModel() })
            return
        }
        try {
            val results = getFilteredListingsUseCase(q = q, categoryId = catId)
            updateSections(results.map { it.toUiModel() })
        } catch (e: Exception) {
            _uiState.value = HomeUiState.Error(
                e.toUserFriendlyMessage(fallback = "Search failed")
            )
        }
    }
}
