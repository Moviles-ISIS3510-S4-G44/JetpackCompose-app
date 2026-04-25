package com.university.marketplace.di

import com.university.marketplace.data.CategoriesRepository
import com.university.marketplace.data.DefaultInteractionsRepository
import com.university.marketplace.data.InteractionsRepository
import com.university.marketplace.data.ListingsRepository
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.domain.CategoryRepository
import com.university.marketplace.domain.ListingRepository
import com.university.marketplace.domain.usecase.GetActiveListingsUseCase
import com.university.marketplace.domain.usecase.GetFilteredListingsUseCase
import com.university.marketplace.domain.usecase.GetListingByIdUseCase
import com.university.marketplace.domain.usecase.SearchListingsByRelevanceUseCase

interface AppContainer {
    val listingRepository: ListingRepository
    val categoryRepository: CategoryRepository
    val interactionsRepository: InteractionsRepository
    val locationRepository: LocationRepository
    val getActiveListingsUseCase: GetActiveListingsUseCase
    val getListingByIdUseCase: GetListingByIdUseCase
    val searchListingsByRelevanceUseCase: SearchListingsByRelevanceUseCase
    val getFilteredListingsUseCase: GetFilteredListingsUseCase
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
        SearchListingsByRelevanceUseCase(listingRepository)
    }

    override val getFilteredListingsUseCase: GetFilteredListingsUseCase by lazy {
        GetFilteredListingsUseCase(listingRepository)
    }
}
