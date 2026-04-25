package com.university.marketplace.domain.usecase

import com.university.marketplace.domain.Purchase
import com.university.marketplace.domain.PurchaseRepository

class CreatePurchaseUseCase(private val repository: PurchaseRepository) {
    suspend operator fun invoke(listingId: String): Purchase = repository.createPurchase(listingId)
}
