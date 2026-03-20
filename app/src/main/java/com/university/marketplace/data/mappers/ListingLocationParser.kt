package com.university.marketplace.data.mappers

import com.university.marketplace.data.api.LocationDto

data class ListingCoordinates(
    val latitude: Double,
    val longitude: Double
)

object ListingLocationParser {

    fun parse(rawLocation: Any?): ListingCoordinates? {
        return when (rawLocation) {
            null -> null
            is String -> parseFromString(rawLocation)
            is Map<*, *> -> parseFromMap(rawLocation)
            is LocationDto -> create(rawLocation.latitude, rawLocation.longitude)
            else -> null
        }
    }

    private fun parseFromString(raw: String): ListingCoordinates? {
        val parts = raw.split(",")
        if (parts.size != 2) return null

        val latitude = parts[0].trim().toDoubleOrNull() ?: return null
        val longitude = parts[1].trim().toDoubleOrNull() ?: return null
        return create(latitude, longitude)
    }

    private fun parseFromMap(raw: Map<*, *>): ListingCoordinates? {
        val latitude = raw["latitude"].toDoubleOrNull() ?: raw["lat"].toDoubleOrNull() ?: return null
        val longitude = raw["longitude"].toDoubleOrNull() ?: raw["lng"].toDoubleOrNull() ?: return null
        return create(latitude, longitude)
    }

    private fun create(latitude: Double, longitude: Double): ListingCoordinates? {
        if (latitude !in -90.0..90.0) return null
        if (longitude !in -180.0..180.0) return null
        return ListingCoordinates(latitude = latitude, longitude = longitude)
    }

    private fun Any?.toDoubleOrNull(): Double? {
        return when (this) {
            is Number -> this.toDouble()
            is String -> this.trim().parseDoubleOrNull()
            else -> null
        }
    }

    private fun String.parseDoubleOrNull(): Double? {
        return try {
            java.lang.Double.parseDouble(this)
        } catch (_: NumberFormatException) {
            null
        }
    }
}

