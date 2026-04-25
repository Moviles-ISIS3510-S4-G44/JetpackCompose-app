package com.university.marketplace.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.toUserFriendlyMessage
import com.university.marketplace.domain.Purchase
import com.university.marketplace.domain.usecase.GetMyPurchasesUseCase
import com.university.marketplace.domain.usecase.RateSellerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PurchaseHistoryUiState {
    data object Loading : PurchaseHistoryUiState
    data object Empty : PurchaseHistoryUiState
    data class Success(val purchases: List<Purchase>) : PurchaseHistoryUiState
    data class Error(val message: String) : PurchaseHistoryUiState
}

class PurchaseHistoryViewModel(
    private val getMyPurchasesUseCase: GetMyPurchasesUseCase,
    private val rateSellerUseCase: RateSellerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PurchaseHistoryUiState>(PurchaseHistoryUiState.Loading)
    val uiState: StateFlow<PurchaseHistoryUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = PurchaseHistoryUiState.Loading
            try {
                val purchases = getMyPurchasesUseCase()
                _uiState.value = if (purchases.isEmpty()) PurchaseHistoryUiState.Empty
                else PurchaseHistoryUiState.Success(purchases)
            } catch (e: Exception) {
                _uiState.value = PurchaseHistoryUiState.Error(
                    e.toUserFriendlyMessage(fallback = "Failed to load purchases")
                )
            }
        }
    }

    fun rateSeller(purchaseId: String, rating: Int) {
        viewModelScope.launch {
            try {
                val updated = rateSellerUseCase(purchaseId, rating)
                val current = _uiState.value
                if (current is PurchaseHistoryUiState.Success) {
                    _uiState.update {
                        PurchaseHistoryUiState.Success(
                            current.purchases.map { if (it.id == purchaseId) updated else it }
                        )
                    }
                }
            } catch (_: Exception) { }
        }
    }
}
