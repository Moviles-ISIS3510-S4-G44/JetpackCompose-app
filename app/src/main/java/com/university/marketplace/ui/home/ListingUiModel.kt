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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val condition: String = "Used",
    val sellerName: String = "Alex Johnson",
    val locationName: String = "Campus",
    val distance: String? = null
)
