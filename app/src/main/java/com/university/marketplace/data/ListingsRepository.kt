package com.university.marketplace.data

import com.university.marketplace.data.api.ListingDto
import com.university.marketplace.data.api.ListingsApi
import com.university.marketplace.data.api.NetworkModule

class ListingsRepository(
    private val api: ListingsApi = NetworkModule.listingsApi
) {
    suspend fun getListings(): List<ListingDto> {
        return api.getListings()
    }

    suspend fun getListingById(id: String): ListingDto {
        return api.getListingById(id)
    }
}
