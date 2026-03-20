package com.university.marketplace.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.university.marketplace.di.AppContainer
import com.university.marketplace.map.MapViewModel
import com.university.marketplace.ui.home.HomeViewModel
import com.university.marketplace.ui.home.ListingDetailViewModel

class MarketplaceViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    getActiveListingsUseCase = container.getActiveListingsUseCase,
                    searchListingsByRelevanceUseCase = container.searchListingsByRelevanceUseCase
                ) as T
            }
            modelClass.isAssignableFrom(ListingDetailViewModel::class.java) -> {
                ListingDetailViewModel(getListingByIdUseCase = container.getListingByIdUseCase) as T
            }
            modelClass.isAssignableFrom(MapViewModel::class.java) -> {
                MapViewModel(getListingByIdUseCase = container.getListingByIdUseCase) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

