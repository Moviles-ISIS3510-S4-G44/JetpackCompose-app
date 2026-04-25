package com.university.marketplace.di

import android.content.Context
import com.university.marketplace.data.CategoriesRepository
import com.university.marketplace.data.ChatRepositoryImpl
import com.university.marketplace.data.DefaultInteractionsRepository
import com.university.marketplace.data.InteractionsRepository
import com.university.marketplace.data.ListingsRepository
import com.university.marketplace.data.PurchasesRepository
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.data.local.AppDatabase
import com.university.marketplace.data.location.AndroidLocationRepository
import com.university.marketplace.data.location.LocationRepository
import com.university.marketplace.data.search.SemanticSearchEngine
import com.university.marketplace.domain.CategoryRepository
import com.university.marketplace.domain.ChatRepository
import com.university.marketplace.domain.ListingRepository
import com.university.marketplace.domain.PurchaseRepository
import com.university.marketplace.domain.usecase.CreatePurchaseUseCase
import com.university.marketplace.domain.usecase.GetActiveListingsUseCase
import com.university.marketplace.domain.usecase.GetFilteredListingsUseCase
import com.university.marketplace.domain.usecase.GetListingByIdUseCase
import com.university.marketplace.domain.usecase.GetMyListingsUseCase
import com.university.marketplace.domain.usecase.GetMyPurchasesUseCase
import com.university.marketplace.domain.usecase.GetSalesHistoryUseCase
import com.university.marketplace.domain.usecase.RateSellerUseCase
import com.university.marketplace.domain.usecase.SearchListingsByRelevanceUseCase

interface AppContainer {
    val listingRepository: ListingRepository
    val categoryRepository: CategoryRepository
    val locationRepository: LocationRepository
    val purchaseRepository: PurchaseRepository
    val interactionsRepository: InteractionsRepository
    val chatRepository: ChatRepository
    val getActiveListingsUseCase: GetActiveListingsUseCase
    val getListingByIdUseCase: GetListingByIdUseCase
    val searchListingsByRelevanceUseCase: SearchListingsByRelevanceUseCase
    val getFilteredListingsUseCase: GetFilteredListingsUseCase
    val getMyListingsUseCase: GetMyListingsUseCase
    val createPurchaseUseCase: CreatePurchaseUseCase
    val getMyPurchasesUseCase: GetMyPurchasesUseCase
    val getSalesHistoryUseCase: GetSalesHistoryUseCase
    val rateSellerUseCase: RateSellerUseCase
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    private val semanticSearchEngine: SemanticSearchEngine by lazy {
        SemanticSearchEngine(context)
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

    override val locationRepository: LocationRepository by lazy {
        AndroidLocationRepository(context)
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

    override val getMyListingsUseCase: GetMyListingsUseCase by lazy {
        GetMyListingsUseCase(listingRepository)
    }

    override val purchaseRepository: PurchaseRepository by lazy {
        PurchasesRepository(api = NetworkModule.purchasesApi)
    }

    override val createPurchaseUseCase: CreatePurchaseUseCase by lazy {
        CreatePurchaseUseCase(purchaseRepository)
    }

    override val getMyPurchasesUseCase: GetMyPurchasesUseCase by lazy {
        GetMyPurchasesUseCase(purchaseRepository)
    }

    override val getSalesHistoryUseCase: GetSalesHistoryUseCase by lazy {
        GetSalesHistoryUseCase(purchaseRepository)
    }

    override val rateSellerUseCase: RateSellerUseCase by lazy {
        RateSellerUseCase(purchaseRepository)
    }

    override val chatRepository: ChatRepository by lazy {
        ChatRepositoryImpl(api = NetworkModule.chatApi)
    }
}
