package com.university.marketplace.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.toUserFriendlyMessage
import com.university.marketplace.domain.Purchase
import com.university.marketplace.domain.usecase.GetSalesHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SalesHistoryUiState {
    data object Loading : SalesHistoryUiState
    data object Empty : SalesHistoryUiState
    data class Success(val sales: List<Purchase>) : SalesHistoryUiState
    data class Error(val message: String) : SalesHistoryUiState
}

class SalesHistoryViewModel(
    private val getSalesHistoryUseCase: GetSalesHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SalesHistoryUiState>(SalesHistoryUiState.Loading)
    val uiState: StateFlow<SalesHistoryUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = SalesHistoryUiState.Loading
            try {
                val sales = getSalesHistoryUseCase()
                _uiState.value = if (sales.isEmpty()) SalesHistoryUiState.Empty
                else SalesHistoryUiState.Success(sales)
            } catch (e: Exception) {
                _uiState.value = SalesHistoryUiState.Error(
                    e.toUserFriendlyMessage(fallback = "Failed to load sales history")
                )
            }
        }
    }
}
