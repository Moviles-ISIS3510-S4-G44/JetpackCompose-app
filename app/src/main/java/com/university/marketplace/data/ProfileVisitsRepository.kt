package com.university.marketplace.data

import android.util.Log
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.data.api.ProfileVisitsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ProfileVisitsRepository {
    suspend fun registerProfileVisit(visitedUserId: String)
}

class DefaultProfileVisitsRepository(
    private val api: ProfileVisitsApi = NetworkModule.profileVisitsApi
) : ProfileVisitsRepository {
    override suspend fun registerProfileVisit(visitedUserId: String) {
        withContext(Dispatchers.IO) {
            try {
                val response = api.registerProfileVisit(visitedUserId)
                if (!response.isSuccessful) {
                    Log.w(
                        TAG,
                        "Profile visit register failed with HTTP ${response.code()} for user $visitedUserId"
                    )
                }
            } catch (error: Throwable) {
                Log.w(TAG, "Profile visit register threw for user $visitedUserId", error)
            }
        }
    }

    private companion object {
        const val TAG = "ProfileVisitsRepository"
    }
}
