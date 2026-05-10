package com.university.marketplace.ui.home

import android.location.Location
import com.university.marketplace.domain.Listing
import java.util.Locale

fun Listing.toUiModel(userLocation: Location? = null): ListingUiModel {
    val baseLocation = locationName ?: "Campus"
    
    val lLat = latitude
    val lLon = longitude
    val distanceStr = if (lLat != null && lLon != null && userLocation != null) {
        val dest = Location("dest").apply {
            latitude = lLat
            longitude = lLon
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

    return ListingUiModel(
        id = id,
        name = title,
        price = price,
        imageUrl = images.firstOrNull().orEmpty(),
        description = description,
        category = categoryId,
        latitude = latitude,
        longitude = longitude,
        condition = condition,
        sellerName = "Seller $sellerId",
        locationName = baseLocation,
        distance = distanceStr
    )
}
