package com.university.marketplace.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.usecase.GetActiveListingsUseCase
import com.university.marketplace.domain.usecase.SearchListingsByRelevanceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getActiveListingsUseCase: GetActiveListingsUseCase,
    private val searchListingsByRelevanceUseCase: SearchListingsByRelevanceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allListings: List<Listing> = emptyList()

    init {
        loadListings()
    }

    fun loadListings() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                allListings = getActiveListingsUseCase()
                updateSections(allListings.map { it.toUiModel() })
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    private fun updateSections(listings: List<ListingUiModel>) {
        val featured = listings.take(4)
        val recent = listings.drop(4)
        _uiState.value = HomeUiState.Success(
            featured = featured,
            recent = recent,
            isSearching = _searchQuery.value.isNotEmpty()
        )
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        val filtered = if (query.isEmpty()) {
            allListings
        } else {
            searchListingsByRelevanceUseCase.execute(allListings, query)
        }
        updateSections(filtered.map { it.toUiModel() })
    }
}
