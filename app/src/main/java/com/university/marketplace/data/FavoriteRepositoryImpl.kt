package com.university.marketplace.data

import android.content.Context
import androidx.work.*
import com.university.marketplace.data.local.*
import com.university.marketplace.data.search.SemanticSearchEngine
import com.university.marketplace.data.sync.SyncFavoritesWorker
import com.university.marketplace.domain.FavoriteRepository
import com.university.marketplace.domain.Listing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FavoriteRepositoryImpl(
    private val favoriteDao: FavoriteDao,
    private val listingDao: ListingDao,
    private val favoriteActionDao: FavoriteActionDao,
    private val semanticSearchEngine: SemanticSearchEngine,
    private val context: Context
) : FavoriteRepository {

    override fun getFavoriteListingIds(): Flow<List<String>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.listingId }
        }
    }

    override suspend fun toggleFavorite(listingId: String) {
        withContext(Dispatchers.IO) {
            val isFav = favoriteDao.isFavorite(listingId).first()
            if (isFav) {
                favoriteDao.deleteFavorite(FavoriteEntity(listingId))
                favoriteActionDao.insertAction(FavoriteActionEntity(listingId = listingId, action = "REMOVE"))
            } else {
                favoriteDao.insertFavorite(FavoriteEntity(listingId))
                favoriteActionDao.insertAction(FavoriteActionEntity(listingId = listingId, action = "ADD"))
            }
            scheduleSync()
        }
    }

    override fun isFavorite(listingId: String): Flow<Boolean> {
        return favoriteDao.isFavorite(listingId)
    }

    override suspend fun getRecommendations(): List<Listing> = withContext(Dispatchers.Default) {
        val favoriteIds = favoriteDao.getAllFavoriteIds()
        if (favoriteIds.isEmpty()) return@withContext emptyList()

        val allListings = listingDao.getActiveListingsList()
        val favorites = allListings.filter { it.id in favoriteIds }
        val candidates = allListings.filter { it.id !in favoriteIds }

        if (favorites.isEmpty()) return@withContext emptyList()

        candidates.map { candidate ->
            var maxSimilarity = 0f
            favorites.forEach { fav ->
                if (candidate.embedding != null && fav.embedding != null) {
                    val sim = semanticSearchEngine.calculateCosineSimilarity(candidate.embedding, fav.embedding)
                    if (sim > maxSimilarity) maxSimilarity = sim
                }
            }
            candidate to maxSimilarity
        }
        .filter { it.second > 0.5f }
        .sortedByDescending { it.second }
        .take(5)
        .map { it.first.toDomain() }
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncFavoritesWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncFavorites",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
