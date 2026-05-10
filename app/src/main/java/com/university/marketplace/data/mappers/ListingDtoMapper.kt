package com.university.marketplace.data.mappers

import com.university.marketplace.data.api.ListingDto
import com.university.marketplace.domain.Listing

fun ListingDto.toDomain(): Listing {
    val coordinates = ListingLocationParser.parse(location)
    
    val nameFromLocation = when (val loc = location) {
        is String -> {
            if (loc.contains("|")) {
                loc.substringAfter("|").trim()
            } else if (coordinates == null) {
                loc
            } else {
                null
            }
        }
        is Map<*, *> -> {
            (loc["name"] ?: loc["label"] ?: loc["location"] ?: loc["address"])?.toString()
        }
        else -> null
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
        longitude = coordinates?.longitude,
        locationName = nameFromLocation
    )
}

