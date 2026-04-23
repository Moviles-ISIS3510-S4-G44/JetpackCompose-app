package com.university.marketplace.domain.usecase

import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.ListingRepository

class GetFilteredListingsUseCase(private val repository: ListingRepository) {
    suspend operator fun invoke(
        q: String? = null,
        categoryId: String? = null,
        condition: String? = null,
        minPrice: Int? = null,
        maxPrice: Int? = null
    ): List<Listing> = repository.searchListings(
        q = q,
        categoryId = categoryId,
        condition = condition,
        minPrice = minPrice,
        maxPrice = maxPrice
    )
}
