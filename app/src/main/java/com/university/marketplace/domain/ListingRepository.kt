package com.university.marketplace.domain

import com.university.marketplace.data.api.SearchIntent
import kotlinx.coroutines.flow.Flow

interface ListingRepository {
    fun getActiveListings(): Flow<List<Listing>>
    suspend fun refreshListings()
    suspend fun getListingById(id: String): Listing
    suspend fun searchListings(query: String): Flow<List<Listing>>
    suspend fun searchListingsFiltered(
        q: String? = null,
        categoryId: String? = null,
        condition: String? = null,
        minPrice: Int? = null,
        maxPrice: Int? = null
    ): List<Listing>
    fun searchListingsFlow(query: String): Flow<List<Listing>>
    suspend fun getMyListings(): List<Listing>
    suspend fun getListingsBySellerId(sellerId: String): List<Listing>
    suspend fun createListing(
        sellerId: String,
        categoryId: String,
        title: String,
        description: String,
        price: Int,
        condition: String,
        images: List<String>,
        location: String,
        locationName: String? = null
    ): Listing

    suspend fun saveListing(listing: Listing)
}
