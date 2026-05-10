package com.university.marketplace.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.university.marketplace.data.api.FavoriteCreateDto
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.data.local.AppDatabase

class SyncFavoritesWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        NetworkModule.initialize(applicationContext)

        val database = AppDatabase.getDatabase(applicationContext)
        val actionDao = database.favoriteActionDao()
        val api = NetworkModule.favoritesApi

        val pendingActions = actionDao.getPendingActions()
        if (pendingActions.isEmpty()) return Result.success()

        Log.d(TAG, "Syncing ${pendingActions.size} favorite actions")

        for (action in pendingActions) {
            try {
                when (action.action) {
                    "ADD" -> {
                        val response = api.addFavorite(FavoriteCreateDto(listingId = action.listingId))
                        if (!response.isSuccessful) {
                            Log.w(TAG, "ADD failed: ${response.code()} for ${action.listingId}")
                            return Result.retry()
                        }
                    }
                    "REMOVE" -> {
                        val response = api.removeFavorite(action.listingId)
                        if (!response.isSuccessful) {
                            Log.w(TAG, "REMOVE failed: ${response.code()} for ${action.listingId}")
                            return Result.retry()
                        }
                    }
                }
                actionDao.deleteAction(action)
            } catch (e: Exception) {
                Log.w(TAG, "Favorite sync error", e)
                return Result.retry()
            }
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "SyncFavoritesWorker"
    }
}
