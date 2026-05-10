package com.university.marketplace.ui.home

import android.location.Location
import com.university.marketplace.domain.Listing
import java.util.Locale

fun Listing.toUiModel(userLocation: Location? = null): ListingUiModel {
    val baseLocation = locationName ?: "Campus"
    val distanceStr = calculateDistanceString(baseLocation, latitude, longitude, userLocation)

    return ListingUiModel(
        id = id,
        sellerId = sellerId,
        name = title,
        price = price,
        imageUrl = images.firstOrNull().orEmpty(),
        description = description,
        category = categoryId,
        latitude = latitude,
        longitude = longitude,
        condition = condition,
        sellerName = sellerName ?: "Seller $sellerId",
        locationName = baseLocation,
        distance = distanceStr
    )
}

fun calculateDistanceString(
    baseLocation: String,
    latitude: Double?,
    longitude: Double?,
    userLocation: Location?
): String {
    return if (latitude != null && longitude != null && userLocation != null) {
        val dest = Location("dest").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        val distanceMeters = userLocation.distanceTo(dest)
        if (distanceMeters < 1000) {
            "$baseLocation • ${distanceMeters.toInt()}m"
        } else {
            String.format(Locale.US, "%s • %.1f km", baseLocation, distanceMeters / 1000f)
        }
    } else {
        baseLocation
    }
}
