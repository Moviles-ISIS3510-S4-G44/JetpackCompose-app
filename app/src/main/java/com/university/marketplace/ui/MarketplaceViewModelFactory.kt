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
import com.university.marketplace.ui.profile.OtherUserProfileViewModel
import com.university.marketplace.ui.chat.ConversationListViewModel
import com.university.marketplace.ui.favorites.FavoritesViewModel
import com.university.marketplace.ui.purchases.PurchaseHistoryViewModel
import com.university.marketplace.ui.purchases.SalesHistoryViewModel

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
                    categoryRepository = container.categoryRepository,
                    locationRepository = container.locationRepository
                ) as T
            }
            modelClass.isAssignableFrom(ListingDetailViewModel::class.java) -> {
                ListingDetailViewModel(
                    getListingByIdUseCase = container.getListingByIdUseCase,
                    interactionsRepository = container.interactionsRepository,
                    favoriteRepository = container.favoriteRepository,
                    locationRepository = container.locationRepository,
                    createPurchaseUseCase = container.createPurchaseUseCase
                ) as T
            }
            modelClass.isAssignableFrom(MapViewModel::class.java) -> {
                MapViewModel(
                    getListingByIdUseCase = container.getListingByIdUseCase,
                    locationRepository = container.locationRepository
                ) as T
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
            modelClass.isAssignableFrom(OtherUserProfileViewModel::class.java) -> {
                OtherUserProfileViewModel(
                    getListingsBySellerUseCase = container.getListingsBySellerUseCase,
                    profileVisitsRepository = container.profileVisitsRepository,
                    authRepository = authRepository
                ) as T
            }
            modelClass.isAssignableFrom(PurchaseHistoryViewModel::class.java) -> {
                PurchaseHistoryViewModel(
                    getMyPurchasesUseCase = container.getMyPurchasesUseCase,
                    rateSellerUseCase = container.rateSellerUseCase
                ) as T
            }
            modelClass.isAssignableFrom(SalesHistoryViewModel::class.java) -> {
                SalesHistoryViewModel(
                    getSalesHistoryUseCase = container.getSalesHistoryUseCase
                ) as T
            }
            modelClass.isAssignableFrom(ConversationListViewModel::class.java) -> {
                ConversationListViewModel(chatRepository = container.chatRepository) as T
            }
            modelClass.isAssignableFrom(FavoritesViewModel::class.java) -> {
                FavoritesViewModel(
                    favoriteRepository = container.favoriteRepository,
                    listingRepository = container.listingRepository,
                    locationRepository = container.locationRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
