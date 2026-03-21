package com.university.marketplace.data.mappers

import android.util.Log
import com.university.marketplace.data.api.ListingDto
import com.university.marketplace.domain.Listing

private const val TAG = "ListingDtoMapper"

fun ListingDto.toDomain(): Listing {
    val coordinates = ListingLocationParser.parse(location)
    if (coordinates == null && location != null && Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Skipping invalid location for listing id=$id. rawLocation=$location")
    }

    return Listing(
        id = id,
        sellerId = sellerId,
        categoryId = categoryId,
        title = title,
        description = description,
        price = price,
        condition = condition,
        images = images,
        status = status,
        latitude = coordinates?.latitude,
        longitude = coordinates?.longitude
    )
}

