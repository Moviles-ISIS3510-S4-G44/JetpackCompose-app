package com.university.marketplace.domain.usecase

import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.ListingRepository

class GetListingByIdUseCase(
    private val repository: ListingRepository
) {
    suspend operator fun invoke(id: String): Listing = repository.getListingById(id)
}

