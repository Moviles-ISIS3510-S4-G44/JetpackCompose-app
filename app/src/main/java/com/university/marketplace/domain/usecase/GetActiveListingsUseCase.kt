package com.university.marketplace.domain.usecase

import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.ListingRepository

class GetActiveListingsUseCase(
    private val repository: ListingRepository
) {
    suspend operator fun invoke(): List<Listing> = repository.getActiveListings()
}

