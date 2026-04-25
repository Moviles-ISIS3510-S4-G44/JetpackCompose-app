package com.university.marketplace.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.toUserFriendlyMessage
import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.usecase.GetActiveListingsUseCase
import com.university.marketplace.domain.usecase.SearchListingsByRelevanceUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var searchJob: Job? = null
    private var userLocation: android.location.Location? = null
    private var allListings: List<Listing> = emptyList()
    private var currentSearchResults: List<Listing>? = null

    private var selectedCategory: String = "Todo"
    private var selectedCondition: String = "Todos"
    private var selectedPriceCap: Int? = null

    private val listingInterestWeights = mutableMapOf<String, Float>()
    private val categoryInterestWeights = mutableMapOf<String, Float>()

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
        val featured = listings.take(4)
        val recent = listings.drop(4)
        _uiState.value = HomeUiState.Success(
            featured = featured,
            recent = recent,
            isSearching = _searchQuery.value.isNotEmpty(),
            selectedCategory = selectedCategory,
            selectedCondition = selectedCondition,
            selectedPriceCap = selectedPriceCap
        )
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun applyFiltersAndPublish() {
        viewModelScope.launch {
            val baseListings = currentSearchResults ?: allListings.ifEmpty { getActiveListingsUseCase().first() }
            val filtered = baseListings
                .asSequence()
                .filter { listing -> matchesCategoryFilter(listing, selectedCategory) }
                .filter { listing -> matchesConditionFilter(listing, selectedCondition) }
                .filter { listing -> selectedPriceCap == null || listing.price <= selectedPriceCap!!.toDouble() }
                .toList()

            val weighted = applyUserBehaviorWeights(filtered)
            updateSections(weighted.map { it.toUiModelWithDistance() })
        }
    }

    private fun applyUserBehaviorWeights(listings: List<Listing>): List<Listing> {
        if (listings.isEmpty()) return emptyList()
        return listings.sortedByDescending { listing: Listing ->
            val byListing = (listingInterestWeights[listing.id] ?: 0f).toDouble()
            val byCategory = (categoryInterestWeights[listing.categoryId.trim().lowercase()] ?: 0f).toDouble()
            val recencyBoost = max(0.0, 1.0 - (listing.price / 10000000.0))
            byListing + byCategory + (recencyBoost * 0.05)
        }
    }

    private fun matchesCategoryFilter(listing: Listing, categoryFilter: String): Boolean {
        if (categoryFilter == "Todo") return true
        val text = "${listing.title} ${listing.description} ${listing.categoryId}".lowercase()
        return when (categoryFilter) {
            "Electrónica" -> listOf("elect", "laptop", "monitor", "teclado", "camara", "audif", "airpod", "raspberry").any { text.contains(it) }
            "Libros" -> listOf("libro", "book", "soledad", "kindle").any { text.contains(it) }
            "Muebles" -> listOf("escritorio", "mesa", "mueble", "lampara").any { text.contains(it) }
            "Accesorios" -> listOf("mochila", "cable", "usb", "airpod", "audif").any { text.contains(it) }
            else -> true
        }
    }

    private fun matchesConditionFilter(listing: Listing, conditionFilter: String): Boolean {
        return when (conditionFilter) {
            "Todos" -> true
            "Nuevo" -> listing.condition.equals("new", ignoreCase = true)
            "Usado" -> listing.condition.equals("used", ignoreCase = true)
            "Reacondicionado" -> listing.condition.equals("refurbished", ignoreCase = true)
            else -> true
        }
    }
}
