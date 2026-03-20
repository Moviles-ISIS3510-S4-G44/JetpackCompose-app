package com.university.marketplace.data.api

import com.squareup.moshi.Json
import java.math.BigDecimal

data class ListingDto(
    val id: String,
    @Json(name = "seller_id") val sellerId: String,
    @Json(name = "category_id") val categoryId: String,
    val title: String,
    val description: String,
    val price: Double, // Moshi handles Double for BigDecimal-compatible numbers by default
    val condition: String,
    val images: List<String>,
    val status: String,
    val location: String
)
