package com.university.marketplace.data

import com.university.marketplace.data.api.CategoriesApi
import com.university.marketplace.domain.Category
import com.university.marketplace.domain.CategoryRepository

class CategoriesRepository(
    private val api: CategoriesApi
) : CategoryRepository {
    private var cache: List<Category>? = null

    override suspend fun getCategories(): List<Category> {
        cache?.let { return it }
        return api.getCategories()
            .map { Category(id = it.id, name = it.name) }
            .also { cache = it }
    }
}
