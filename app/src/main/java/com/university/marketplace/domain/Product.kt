package com.university.marketplace.domain

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double = 4.5,
    val isFeatured: Boolean = false,
    val category: String = "General",
    val locationLabel: String = "Campus"
)
