package com.university.marketplace.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.university.marketplace.data.auth.AuthRepository
import com.university.marketplace.di.AppContainer
import com.university.marketplace.map.MapViewModel
import com.university.marketplace.ui.home.CreateListingViewModel
import com.university.marketplace.ui.home.HomeViewModel
import com.university.marketplace.ui.home.ListingDetailViewModel
import com.university.marketplace.ui.profile.MyListingsViewModel

class MarketplaceViewModelFactory(
    private val container: AppContainer,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    getActiveListingsUseCase = container.getActiveListingsUseCase,
                    searchListingsByRelevanceUseCase = container.searchListingsByRelevanceUseCase,
                    getFilteredListingsUseCase = container.getFilteredListingsUseCase,
                    categoryRepository = container.categoryRepository
                ) as T
            }
            modelClass.isAssignableFrom(ListingDetailViewModel::class.java) -> {
                ListingDetailViewModel(
                    getListingByIdUseCase = container.getListingByIdUseCase,
                    interactionsRepository = container.interactionsRepository
                ) as T
            }
            modelClass.isAssignableFrom(MapViewModel::class.java) -> {
                MapViewModel(getListingByIdUseCase = container.getListingByIdUseCase) as T
            }
            modelClass.isAssignableFrom(CreateListingViewModel::class.java) -> {
                CreateListingViewModel(
                    listingRepository = container.listingRepository,
                    categoryRepository = container.categoryRepository,
                    authRepository = authRepository
                ) as T
            }
            modelClass.isAssignableFrom(MyListingsViewModel::class.java) -> {
                MyListingsViewModel(
                    getMyListingsUseCase = container.getMyListingsUseCase
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
