package com.university.marketplace.domain.usecase

import com.university.marketplace.domain.Purchase
import com.university.marketplace.domain.PurchaseRepository

class RateSellerUseCase(private val repository: PurchaseRepository) {
    suspend operator fun invoke(purchaseId: String, rating: Int): Purchase =
        repository.rateSeller(purchaseId, rating)
}
