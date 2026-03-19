package com.university.marketplace.data

import com.university.marketplace.domain.Product

interface ProductRepository {
    fun getProducts(): List<Product>
    fun getFeaturedProducts(): List<Product>
    fun getRecentProducts(): List<Product>
    fun searchProducts(query: String): List<Product>
    fun getProductById(productId: String): Product?
}

