package com.university.marketplace.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface ListingsApi {
    @GET("listings")
    suspend fun getListings(): List<ListingDto>

    @GET("listings/{listing_id}")
    suspend fun getListingById(@Path("listing_id") listingId: String): ListingDto
}
