package com.university.marketplace.ui.home

data class ListingUiModel(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val rating: Double = 4.5,
    val isFeatured: Boolean = false,
    val description: String = "",
    val category: String = "General",
    val latitude: Double = 4.601,
    val longitude: Double = -74.065,
    val condition: String = "Used",
    val sellerName: String = "Alex Johnson",
    val locationName: String = "Campus"
)
