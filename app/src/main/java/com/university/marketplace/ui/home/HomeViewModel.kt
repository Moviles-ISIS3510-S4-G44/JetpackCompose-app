package com.university.marketplace.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.InteractionsRepository
import com.university.marketplace.data.NotificationRepository
import com.university.marketplace.data.auth.AuthRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.max

class HomeViewModel(
    private val getActiveListingsUseCase: GetActiveListingsUseCase,
    private val searchListingsByRelevanceUseCase: SearchListingsByRelevanceUseCase,
    @Suppress("UNUSED_PARAMETER")
    private val getFilteredListingsUseCase: GetFilteredListingsUseCase,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: LocationRepository,
    private val interactionsRepository: InteractionsRepository,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _selectedPriceCap = MutableStateFlow<Int?>(null)
    val selectedPriceCap: StateFlow<Int?> = _selectedPriceCap.asStateFlow()

    private val _selectedLocationSort = MutableStateFlow(LocationSortOption.NONE)
    val selectedLocationSort: StateFlow<LocationSortOption> = _selectedLocationSort.asStateFlow()

    private val _unreadNotificationsCount = MutableStateFlow(0)

    private var allListings: List<Listing> = emptyList()
    private var currentSearchResults: List<Listing>? = null
    private var searchJob: Job? = null
    private var userLocation: Location? = null
    private var recommendedCategoryName: String? = null
    private var recommendedCategoryId: String? = null

    init {
        loadCategories()
        loadTopInteractionAndListings()
        observeLocationUpdates()
        observeNotifications()
        
        userLocation = Location("default").apply {
            latitude = 4.601
            longitude = -74.065
        }
        
        refreshUserLocation()
    }

    private fun observeNotifications() {
        notificationRepository.unreadCount
            .onEach { count ->
                _unreadNotificationsCount.value = count
                applyFiltersAndPublish()
            }
            .launchIn(viewModelScope)
    }

    private fun loadTopInteractionAndListings() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val topInteraction = interactionsRepository.getTopInteraction(userId)
                recommendedCategoryName = topInteraction?.categoryName
                recommendedCategoryId = topInteraction?.categoryId
            }
            observeListings()
            observeSearch()
            loadListings()
        }
    }

    fun refreshUserLocation() {
        viewModelScope.launch {
            val loc = try {
                locationRepository.getLastKnownLocation()
            } catch (e: Exception) {
                null
            }
            
            if (loc != null) {
                updateUserLocation(loc.latitude, loc.longitude)
            }
        }
    }

    private fun observeLocationUpdates() {
        locationRepository.getLocationUpdates()
            .onEach { loc ->
                updateUserLocation(loc.latitude, loc.longitude)
            }
            .launchIn(viewModelScope)
    }

    private fun updateUserLocation(latitude: Double, longitude: Double) {
        userLocation = Location("user").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        applyFiltersAndPublish()
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

    fun onPriceCapSelected(priceCap: Int?) {
        _selectedPriceCap.value = if (_selectedPriceCap.value == priceCap) null else priceCap
        applyFiltersAndPublish()
    }

    fun onLocationSortSelected(option: LocationSortOption) {
        _selectedLocationSort.value = option
        applyFiltersAndPublish()
    }

    fun onListingOpened(listing: ListingUiModel) {
        // Local behavior tracking (optional enhancement)
    }

    private fun applyFiltersAndPublish() {
        val baseListings = currentSearchResults ?: allListings
        val categoryId = _selectedCategoryId.value
        val priceCap = _selectedPriceCap.value
        val sortOption = _selectedLocationSort.value

        val filtered = baseListings
            .asSequence()
            .filter { listing ->
                categoryId == null || listing.categoryId.equals(categoryId, ignoreCase = true)
            }
            .filter { listing ->
                priceCap == null || listing.price <= priceCap.toDouble()
            }
            .toList()

        val processed = when (sortOption) {
            LocationSortOption.NEAREST -> sortByDistance(filtered, nearest = true)
            LocationSortOption.FARTHEST -> sortByDistance(filtered, nearest = false)
            LocationSortOption.NONE -> filtered // Can apply local weights here if desired
        }

        val recommended = if (recommendedCategoryId != null && !isSearching()) {
            allListings.filter { it.categoryId == recommendedCategoryId }.take(5)
        } else emptyList()

        updateSections(
            listings = processed.map { it.toUiModel(userLocation) },
            recommended = recommended.map { it.toUiModel(userLocation) }
        )
    }

    private fun isSearching(): Boolean {
        return _searchQuery.value.isNotEmpty() || _selectedCategoryId.value != null || _selectedPriceCap.value != null || _selectedLocationSort.value != LocationSortOption.NONE
    }

    private fun sortByDistance(listings: List<Listing>, nearest: Boolean): List<Listing> {
        val currentLoc = userLocation ?: return listings
        return listings.sortedWith { a, b ->
            val distA = calculateRawDistance(a, currentLoc)
            val distB = calculateRawDistance(b, currentLoc)
            if (nearest) distA.compareTo(distB) else distB.compareTo(distA)
        }
    }

    private fun calculateRawDistance(listing: Listing, currentLoc: Location): Float {
        if (listing.latitude == null || listing.longitude == null) return Float.MAX_VALUE
        val dest = Location("dest").apply {
            latitude = listing.latitude
            longitude = listing.longitude
        }
        return currentLoc.distanceTo(dest)
    }

    private fun updateSections(listings: List<ListingUiModel>, recommended: List<ListingUiModel>) {
        val featured = listings.take(4)
        val recent = listings.drop(4)
        _uiState.value = HomeUiState.Success(
            featured = featured,
            recent = recent,
            recommended = recommended,
            recommendedCategoryName = recommendedCategoryName,
            isSearching = isSearching(),
            selectedCategory = _selectedCategoryId.value ?: "Todo",
            selectedPriceCap = _selectedPriceCap.value,
            selectedLocationSort = _selectedLocationSort.value,
            unreadNotificationsCount = _unreadNotificationsCount.value
        )
    }
}
