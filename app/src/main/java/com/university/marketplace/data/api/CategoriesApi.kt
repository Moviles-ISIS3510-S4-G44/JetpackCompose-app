package com.university.marketplace.data.api

import retrofit2.http.GET

interface CategoriesApi {
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>
}
