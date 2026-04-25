package com.university.marketplace.data.api

import com.squareup.moshi.Json

data class PurchaseDto(
    val id: String,
    @Json(name = "listing_id") val listingId: String,
    @Json(name = "buyer_id") val buyerId: String,
    @Json(name = "price_at_purchase") val priceAtPurchase: Int,
    @Json(name = "purchased_at") val purchasedAt: String,
    @Json(name = "seller_rating") val sellerRating: Int?
)

data class CreatePurchaseDto(
    @Json(name = "listing_id") val listingId: String
)
