package com.university.marketplace.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface InteractionsApi {
    @POST("interactions")
    suspend fun registerInteraction(@Body payload: InteractionRequestDto): Response<Unit>
}

data class InteractionRequestDto(
    @SerializedName("listing_id")
    val listingId: String
)
