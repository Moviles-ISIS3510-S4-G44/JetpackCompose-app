package com.university.marketplace.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface PurchasesApi {
    @POST("purchases")
    suspend fun createPurchase(@Body payload: CreatePurchaseDto): PurchaseDto

    @GET("purchases/me")
    suspend fun getMyPurchases(): List<PurchaseDto>

    @GET("purchases/sold")
    suspend fun getMySales(): List<PurchaseDto>

    @PATCH("purchases/{id}/rate-seller")
    suspend fun rateSeller(
        @Path("id") id: String,
        @Body payload: RateSellerDto
    ): PurchaseDto
}
