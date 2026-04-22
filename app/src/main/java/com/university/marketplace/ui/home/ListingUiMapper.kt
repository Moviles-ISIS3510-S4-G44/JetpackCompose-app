package com.university.marketplace.ui.home

import com.university.marketplace.domain.Listing

fun Listing.toUiModel(): ListingUiModel {
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
        locationName = "Campus",
        distance = null
    )
}
