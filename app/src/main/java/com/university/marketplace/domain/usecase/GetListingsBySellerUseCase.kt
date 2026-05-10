package com.university.marketplace.domain.usecase

import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.ListingRepository

class GetListingsBySellerUseCase(
    private val repository: ListingRepository
) {
    suspend operator fun invoke(sellerId: String): List<Listing> = 
        repository.getListingsBySellerId(sellerId)
}
