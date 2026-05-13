package com.university.marketplace.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FavoritesApi {
    @GET("favorites")
    suspend fun getFavorites(): Response<FavoritesListDto>

    @POST("favorites")
    suspend fun addFavorite(@Body body: FavoriteCreateDto): Response<Unit>

    @DELETE("favorites/{listing_id}")
    suspend fun removeFavorite(@Path("listing_id") listingId: String): Response<Unit>
}

data class FavoritesListDto(
    @SerializedName("listing_ids")
    val listingIds: List<String>
)

data class FavoriteCreateDto(
    @SerializedName("listing_id")
    val listingId: String
)
