package com.university.marketplace.domain

data class Listing(
    val id: String,
    val sellerId: String,
    val categoryId: String,
    val title: String,
    val description: String,
    val price: Double,
    val condition: String,
    val images: List<String>,
    val status: String,
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String? = null
)

