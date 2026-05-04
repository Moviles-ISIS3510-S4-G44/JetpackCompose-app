package com.university.marketplace.domain

import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun getFavoriteListingIds(): Flow<List<String>>
    suspend fun toggleFavorite(listingId: String)
    fun isFavorite(listingId: String): Flow<Boolean>
    suspend fun getRecommendations(): List<Listing>
}
