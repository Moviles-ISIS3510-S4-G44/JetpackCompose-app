package com.university.marketplace.data

import com.university.marketplace.domain.Product

class FakeProductRepository {
    private val products = listOf(
        Product(
            id = "1",
            name = "Calculus Textbook 11th Ed",
            price = 45.0,
            imageUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=300&h=300&auto=format&fit=crop",
            latitude = 4.601,
            longitude = -74.065,
            rating = 4.8,
            isFeatured = true,
            category = "Books"
        ),
        Product(
            id = "2",
            name = "MacBook Pro 13-inch M2",
            price = 850.0,
            imageUrl = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=300&h=300&auto=format&fit=crop",
            latitude = 4.602,
            longitude = -74.066,
            rating = 5.0,
            isFeatured = true,
            category = "Electronics"
        ),
        Product(
            id = "3",
            name = "Study Lamp",
            price = 15.0,
            imageUrl = "https://images.unsplash.com/photo-1534073828943-f801091bb18c?q=80&w=300&h=300&auto=format&fit=crop",
            latitude = 4.603,
            longitude = -74.067,
            rating = 4.2,
            isFeatured = false,
            category = "Furniture"
        ),
        Product(
            id = "4",
            name = "Scientific Calculator",
            price = 30.0,
            imageUrl = "https://images.unsplash.com/photo-1574607383476-f517f220d398?q=80&w=300&h=300&auto=format&fit=crop",
            latitude = 4.604,
            longitude = -74.068,
            rating = 4.7,
            isFeatured = false,
            category = "Electronics"
        ),
        Product(
            id = "5",
            name = "Ergonomic Chair",
            price = 120.0,
            imageUrl = "https://images.unsplash.com/photo-1505797149-35ebcb05a6fd?q=80&w=300&h=300&auto=format&fit=crop",
            latitude = 4.605,
            longitude = -74.069,
            rating = 4.9,
            isFeatured = false,
            category = "Furniture"
        )
    )

    fun getProducts(): List<Product> = products
    
    fun getFeaturedProducts(): List<Product> = products.filter { it.isFeatured }
    
    fun getRecentProducts(): List<Product> = products.filter { !it.isFeatured }

    fun searchProducts(query: String): List<Product> {
        return products.filter { it.name.contains(query, ignoreCase = true) }
    }
}
