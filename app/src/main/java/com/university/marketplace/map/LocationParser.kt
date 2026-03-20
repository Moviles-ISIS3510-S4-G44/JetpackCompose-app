package com.university.marketplace.map

import android.util.Log
import com.google.android.gms.maps.model.LatLng

object LocationParser {
    private const val TAG = "LocationParser"

    fun parse(location: Any?): LatLng? {
        if (location == null) return null

        return when (location) {
            is String -> parseString(location)
            is Map<*, *> -> parseMap(location)
            else -> {
                Log.d(TAG, "Unknown location type: ${location::class.java.simpleName}")
                null
            }
        }
    }

    private fun parseString(location: String): LatLng? {
        val parts = location.split(",").map { it.trim() }
        if (parts.size != 2) {
            Log.d(TAG, "Invalid location string format: $location")
            return null
        }

        return try {
            val lat = parts[0].toDouble()
            val lng = parts[1].toDouble()
            createLatLng(lat, lng)
        } catch (e: NumberFormatException) {
            Log.d(TAG, "Failed to parse coordinates from string: $location")
            null
        }
    }

    private fun parseMap(location: Map<*, *>): LatLng? {
        val lat = (location["latitude"] as? Double) ?: (location["lat"] as? Double)
        val lng = (location["longitude"] as? Double) ?: (location["lng"] as? Double)

        if (lat == null || lng == null) {
            Log.d(TAG, "Missing latitude or longitude in map: $location")
            return null
        }

        return createLatLng(lat, lng)
    }

    private fun createLatLng(lat: Double, lng: Double): LatLng? {
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            Log.d(TAG, "Coordinates out of range: lat=$lat, lng=$lng")
            return null
        }
        return LatLng(lat, lng)
    }
}
