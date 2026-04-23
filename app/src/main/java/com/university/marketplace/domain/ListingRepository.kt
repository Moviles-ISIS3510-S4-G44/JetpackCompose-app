package com.university.marketplace.domain

interface ListingRepository {
    suspend fun getActiveListings(): List<Listing>
    suspend fun getListingById(id: String): Listing
    suspend fun searchListings(
        q: String? = null,
        categoryId: String? = null,
        condition: String? = null,
        minPrice: Int? = null,
        maxPrice: Int? = null
    ): List<Listing>
    suspend fun getMyListings(): List<Listing>
    suspend fun createListing(
        sellerId: String,
        categoryId: String,
        title: String,
        description: String,
        price: Int,
        condition: String,
        images: List<String>,
        location: String
    ): Listing
}

