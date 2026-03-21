package com.university.marketplace.map

import com.university.marketplace.ui.home.ListingUiModel

sealed interface MapUiState {
    object Loading : MapUiState
    data class Success(val listing: ListingUiModel) : MapUiState
    data class Error(val message: String) : MapUiState
}

