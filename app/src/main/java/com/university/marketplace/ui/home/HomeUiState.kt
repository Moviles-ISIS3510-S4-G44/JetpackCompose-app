package com.university.marketplace.ui.home

enum class LocationSortOption {
    NONE, NEAREST, FARTHEST
}

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Success(
        val featured: List<ListingUiModel>,
        val recent: List<ListingUiModel>,
        val isSearching: Boolean = false,
        val selectedCategory: String = "Todo",
        val selectedCondition: String = "Todos",
        val selectedPriceCap: Int? = null,
        val selectedLocationSort: LocationSortOption = LocationSortOption.NONE
    ) : HomeUiState
}
