package com.university.marketplace.domain

interface PurchaseRepository {
    suspend fun createPurchase(listingId: String): Purchase
    suspend fun getMyPurchases(): List<Purchase>
    suspend fun getMySales(): List<Purchase>
    suspend fun rateSeller(purchaseId: String, rating: Int): Purchase
}
