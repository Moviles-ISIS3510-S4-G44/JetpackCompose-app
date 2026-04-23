package com.university.marketplace.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ListingsApi {
    @GET("listings")
    suspend fun getListings(
        @Query("q") q: String? = null,
        @Query("category_id") categoryId: String? = null,
        @Query("condition") condition: String? = null,
        @Query("min_price") minPrice: Int? = null,
        @Query("max_price") maxPrice: Int? = null,
        @Query("location") location: String? = null,
        @Query("status") status: String? = null
    ): List<ListingDto>

    @GET("listings/{listing_id}")
    suspend fun getListingById(@Path("listing_id") listingId: String): ListingDto

    @GET("listings/me")
    suspend fun getMyListings(): List<ListingDto>
}
