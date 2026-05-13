package com.university.marketplace.data.api

import com.squareup.moshi.Json

data class ListingDto(
    val id: String,
    @Json(name = "seller_id") val sellerId: String,
    @Json(name = "category_id") val categoryId: String,
    val title: String,
    val description: String,
    val price: Double,
    val condition: String,
    val images: List<String>,
    val status: String,
    val location: Any?, // Can be String or Map<String, Double>
    @Json(name = "location_name") val locationName: String? = null
)

data class LocationDto(
    val latitude: Double,
    val longitude: Double
)
