package com.university.marketplace.data

import com.university.marketplace.data.api.CreateListingDto
import com.university.marketplace.data.api.ListingsApi
import com.university.marketplace.data.api.NetworkModule
import com.university.marketplace.data.mappers.toDomain
import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.ListingRepository

class ListingsRepository(
    private val api: ListingsApi = NetworkModule.listingsApi
) : ListingRepository {
    override suspend fun getActiveListings(): List<Listing> {
        return api.getListings(status = "published").map { it.toDomain() }
    }

    override suspend fun getListingById(id: String): Listing {
        return api.getListingById(id).toDomain()
    }

    override suspend fun searchListings(
        q: String?,
        categoryId: String?,
        condition: String?,
        minPrice: Int?,
        maxPrice: Int?
    ): List<Listing> {
        return api.getListings(
            q = q,
            categoryId = categoryId,
            condition = condition,
            minPrice = minPrice,
            maxPrice = maxPrice,
            status = "published"
        ).map { it.toDomain() }
    }

    override suspend fun getMyListings(): List<Listing> {
        return api.getMyListings().map { it.toDomain() }
    }

    override suspend fun createListing(
        sellerId: String,
        categoryId: String,
        title: String,
        description: String,
        price: Int,
        condition: String,
        images: List<String>,
        location: String
    ): Listing {
        return api.createListing(
            CreateListingDto(
                sellerId = sellerId,
                categoryId = categoryId,
                title = title,
                description = description,
                price = price,
                condition = condition,
                images = images,
                location = location
            )
        ).toDomain()
    }
}
