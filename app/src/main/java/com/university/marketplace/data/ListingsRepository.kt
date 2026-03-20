package com.university.marketplace.data

import com.university.marketplace.data.api.ListingsApi
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.data.mappers.toDomain
import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.ListingRepository

class ListingsRepository(
    private val api: ListingsApi = NetworkModule.listingsApi
) : ListingRepository {
    override suspend fun getActiveListings(): List<Listing> {
        return api.getListings()
            .map { it.toDomain() }
            .filter { it.status.equals("active", ignoreCase = true) }
    }

    override suspend fun getListingById(id: String): Listing {
        return api.getListingById(id).toDomain()
    }
}
