package com.university.marketplace.domain.usecase

import com.university.marketplace.domain.Purchase
import com.university.marketplace.domain.PurchaseRepository

class GetSalesHistoryUseCase(private val repository: PurchaseRepository) {
    suspend operator fun invoke(): List<Purchase> = repository.getMySales()
}
