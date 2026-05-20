package com.university.marketplace.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface InteractionsApi {
    @POST("interactions")
    suspend fun registerInteraction(@Body payload: InteractionRequestDto): Response<Unit>

    @GET("interactions/users/{user_id}/top")
    suspend fun getTopInteractions(@Path("user_id") userId: String): Response<TopInteractionDto>
}

data class InteractionRequestDto(
    @SerializedName("listing_id")
    val listingId: String
)

data class TopInteractionDto(
    @SerializedName("category_id")
    val categoryId: String,
    @SerializedName("category_name")
    val categoryName: String
)
