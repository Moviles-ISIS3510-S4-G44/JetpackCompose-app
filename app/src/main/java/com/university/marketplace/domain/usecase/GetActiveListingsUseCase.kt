package com.university.marketplace.domain.usecase

import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.ListingRepository
import kotlinx.coroutines.flow.Flow

class GetActiveListingsUseCase(
    private val repository: ListingRepository
) {
    operator fun invoke(): Flow<List<Listing>> = repository.getActiveListings()

    suspend fun refresh() {
        repository.refreshListings()
    }
}
