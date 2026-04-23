package com.university.marketplace.di

import com.university.marketplace.data.CategoriesRepository
import com.university.marketplace.data.DefaultInteractionsRepository
import com.university.marketplace.data.InteractionsRepository
import com.university.marketplace.data.ListingsRepository
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.data.auth.AuthRepository
import com.university.marketplace.domain.CategoryRepository
import com.university.marketplace.domain.ListingRepository
import com.university.marketplace.domain.usecase.GetActiveListingsUseCase
import com.university.marketplace.domain.usecase.GetFilteredListingsUseCase
import com.university.marketplace.domain.usecase.GetListingByIdUseCase
import com.university.marketplace.domain.usecase.GetMyListingsUseCase
import com.university.marketplace.domain.usecase.SearchListingsByRelevanceUseCase

interface AppContainer {
    val listingRepository: ListingRepository
    val categoryRepository: CategoryRepository
    val interactionsRepository: InteractionsRepository
    val getActiveListingsUseCase: GetActiveListingsUseCase
    val getListingByIdUseCase: GetListingByIdUseCase
    val searchListingsByRelevanceUseCase: SearchListingsByRelevanceUseCase
    val getFilteredListingsUseCase: GetFilteredListingsUseCase
    val getMyListingsUseCase: GetMyListingsUseCase
}

class DefaultAppContainer : AppContainer {
    override val listingRepository: ListingRepository by lazy {
        ListingsRepository(api = NetworkModule.listingsApi)
    }

    override val categoryRepository: CategoryRepository by lazy {
        CategoriesRepository(api = NetworkModule.categoriesApi)
    }

    override val interactionsRepository: InteractionsRepository by lazy {
        DefaultInteractionsRepository(api = NetworkModule.interactionsApi)
    }

    override val getActiveListingsUseCase: GetActiveListingsUseCase by lazy {
        GetActiveListingsUseCase(listingRepository)
    }

    override val getListingByIdUseCase: GetListingByIdUseCase by lazy {
        GetListingByIdUseCase(listingRepository)
    }

    override val searchListingsByRelevanceUseCase: SearchListingsByRelevanceUseCase by lazy {
        SearchListingsByRelevanceUseCase()
    }

    override val getFilteredListingsUseCase: GetFilteredListingsUseCase by lazy {
        GetFilteredListingsUseCase(listingRepository)
    }

    override val getMyListingsUseCase: GetMyListingsUseCase by lazy {
        GetMyListingsUseCase(listingRepository)
    }
}
