package com.university.marketplace.di

import com.university.marketplace.data.ListingsRepository
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.domain.ListingRepository
import com.university.marketplace.domain.usecase.GetActiveListingsUseCase
import com.university.marketplace.domain.usecase.GetListingByIdUseCase
import com.university.marketplace.domain.usecase.SearchListingsByRelevanceUseCase

interface AppContainer {
    val listingRepository: ListingRepository
    val getActiveListingsUseCase: GetActiveListingsUseCase
    val getListingByIdUseCase: GetListingByIdUseCase
    val searchListingsByRelevanceUseCase: SearchListingsByRelevanceUseCase
}

class DefaultAppContainer : AppContainer {
    override val listingRepository: ListingRepository by lazy {
        ListingsRepository(api = NetworkModule.listingsApi)
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
}

