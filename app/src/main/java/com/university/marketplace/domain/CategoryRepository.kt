package com.university.marketplace.domain

interface CategoryRepository {
    suspend fun getCategories(): List<Category>
}
