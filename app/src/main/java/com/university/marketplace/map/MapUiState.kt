package com.university.marketplace.map

import com.university.marketplace.ui.home.ListingUiModel

data class MapUiState(
    val content: Content = Content.Loading,
    val userLocation: Pair<Double, Double>? = null
) {
    sealed interface Content {
        object Loading : Content
        data class Success(val listing: ListingUiModel) : Content
        data class Error(val message: String) : Content
    }
}
