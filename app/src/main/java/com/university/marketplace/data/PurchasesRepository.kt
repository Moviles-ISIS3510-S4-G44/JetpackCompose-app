package com.university.marketplace.data

import com.university.marketplace.data.api.CreatePurchaseDto
import com.university.marketplace.data.api.PurchasesApi
import com.university.marketplace.data.api.RateSellerDto
import com.university.marketplace.domain.Purchase
import com.university.marketplace.domain.PurchaseRepository

class PurchasesRepository(
    private val api: PurchasesApi
) : PurchaseRepository {
    override suspend fun createPurchase(listingId: String): Purchase =
        api.createPurchase(CreatePurchaseDto(listingId)).toDomain()

    override suspend fun getMyPurchases(): List<Purchase> =
        api.getMyPurchases().map { it.toDomain() }

    override suspend fun getMySales(): List<Purchase> =
        api.getMySales().map { it.toDomain() }

    override suspend fun rateSeller(purchaseId: String, rating: Int): Purchase =
        api.rateSeller(purchaseId, RateSellerDto(rating)).toDomain()

    private fun com.university.marketplace.data.api.PurchaseDto.toDomain() = Purchase(
        id = id,
        listingId = listingId,
        buyerId = buyerId,
        priceAtPurchase = priceAtPurchase,
        purchasedAt = purchasedAt,
        sellerRating = sellerRating
    )
}
