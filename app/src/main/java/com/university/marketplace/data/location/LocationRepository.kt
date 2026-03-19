package com.university.marketplace.data.location

data class UserLocation(
    val latitude: Double,
    val longitude: Double
)

@Suppress("unused")
interface LocationRepository {
    @Suppress("unused")
    suspend fun getLastKnownLocation(): UserLocation?
}

