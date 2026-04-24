package com.university.marketplace.domain.usecase

import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.ListingRepository
import kotlinx.coroutines.flow.Flow

class SearchListingsByRelevanceUseCase(
    private val repository: ListingRepository
) {
    suspend fun execute(query: String): Flow<List<Listing>> {
        return repository.searchListings(query)
    }
}
