package com.university.marketplace.domain

data class Purchase(
    val id: String,
    val listingId: String,
    val buyerId: String,
    val priceAtPurchase: Int,
    val purchasedAt: String,
    val sellerRating: Int?
)
