package com.university.marketplace.domain

import com.university.marketplace.data.api.SearchIntent
import kotlinx.coroutines.flow.Flow

interface ListingRepository {
    fun getActiveListings(): Flow<List<Listing>>
    suspend fun refreshListings()
    suspend fun getListingById(id: String): Listing
    suspend fun searchListings(query: String): Flow<List<Listing>>
    suspend fun parseSearchIntent(query: String): SearchIntent?
}
