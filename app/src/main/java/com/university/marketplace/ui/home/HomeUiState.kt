package com.university.marketplace.ui.home

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Success(
        val featured: List<ListingUiModel>,
        val recent: List<ListingUiModel>,
        val isSearching: Boolean = false
    ) : HomeUiState
}
