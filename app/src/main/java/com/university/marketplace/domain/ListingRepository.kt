package com.university.marketplace.domain

interface ListingRepository {
    suspend fun getActiveListings(): List<Listing>
    suspend fun getListingById(id: String): Listing
}

