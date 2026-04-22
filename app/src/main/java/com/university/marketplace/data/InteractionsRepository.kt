package com.university.marketplace.data

import android.util.Log
import com.university.marketplace.data.api.InteractionRequestDto
import com.university.marketplace.data.api.InteractionsApi
import com.university.marketplace.data.api.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface InteractionsRepository {
    suspend fun registerVisit(listingId: String)
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

    private companion object {
        const val TAG = "InteractionsRepository"
    }
}
