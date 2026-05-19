package com.university.marketplace.data.location

import kotlinx.coroutines.flow.Flow

data class UserLocation(
    val latitude: Double,
    val longitude: Double
)

@Suppress("unused")
interface LocationRepository {
    @Suppress("unused")
    suspend fun getLastKnownLocation(): UserLocation?
    fun getLocationUpdates(): Flow<UserLocation>
}

