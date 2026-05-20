package com.university.marketplace.data

import android.util.Log
import com.university.marketplace.data.api.InteractionRequestDto
import com.university.marketplace.data.api.InteractionsApi
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.data.api.TopInteractionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface InteractionsRepository {
    suspend fun registerVisit(listingId: String)
    suspend fun getTopInteraction(userId: String): TopInteractionDto?
}

class DefaultInteractionsRepository(
    private val api: InteractionsApi = NetworkModule.interactionsApi
) : InteractionsRepository {
    override suspend fun registerVisit(listingId: String) {
        withContext(Dispatchers.IO) {
            try {
                val response = api.registerInteraction(
                    InteractionRequestDto(listingId = listingId)
                )
                if (!response.isSuccessful) {
                    Log.w(
                        TAG,
                        "Interaction register failed with HTTP ${response.code()} for listing $listingId"
                    )
                }
            } catch (error: Throwable) {
                Log.w(TAG, "Interaction register threw for listing $listingId", error)
            }
        }
    }

    override suspend fun getTopInteraction(userId: String): TopInteractionDto? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getTopInteractions(userId)
                if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Error getting top interaction for user $userId", error)
                null
            }
        }
    }

    private companion object {
        const val TAG = "InteractionsRepository"
    }
}
