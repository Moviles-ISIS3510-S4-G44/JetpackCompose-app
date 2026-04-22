package com.university.marketplace.di

import android.content.Context
import com.university.marketplace.data.DefaultInteractionsRepository
import com.university.marketplace.data.InteractionsRepository
import com.university.marketplace.data.ListingsRepository
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.data.local.AppDatabase
import com.university.marketplace.data.location.AndroidLocationRepository
import com.university.marketplace.data.location.LocationRepository
import com.university.marketplace.data.search.SemanticSearchEngine
import com.university.marketplace.domain.ListingRepository
import com.university.marketplace.domain.usecase.GetActiveListingsUseCase
import com.university.marketplace.domain.usecase.GetListingByIdUseCase
import com.university.marketplace.domain.usecase.SearchListingsByRelevanceUseCase

interface AppContainer {
    val listingRepository: ListingRepository
    val interactionsRepository: InteractionsRepository
    val locationRepository: LocationRepository
    val getActiveListingsUseCase: GetActiveListingsUseCase
    val getListingByIdUseCase: GetListingByIdUseCase
    val searchListingsByRelevanceUseCase: SearchListingsByRelevanceUseCase
}

class DefaultAppContainer(context: Context) : AppContainer {
    
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    private val semanticSearchEngine: SemanticSearchEngine by lazy {
        SemanticSearchEngine(context)
    }

    override val locationRepository: LocationRepository by lazy {
        AndroidLocationRepository(context)
    }

    override val listingRepository: ListingRepository by lazy {
        ListingsRepository(
            api = NetworkModule.listingsApi,
            groqApi = NetworkModule.groqApi,
            dao = database.listingDao(),
            semanticSearchEngine = semanticSearchEngine
        )
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
        SearchListingsByRelevanceUseCase(listingRepository)
    }
}
