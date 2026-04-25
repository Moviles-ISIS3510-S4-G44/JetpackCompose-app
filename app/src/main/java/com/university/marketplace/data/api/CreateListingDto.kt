package com.university.marketplace.data.api

import com.squareup.moshi.Json

data class CreateListingDto(
    @Json(name = "seller_id") val sellerId: String,
    @Json(name = "category_id") val categoryId: String,
    val title: String,
    val description: String,
    val price: Int,
    val condition: String,
    val images: List<String>,
    val location: String
)
