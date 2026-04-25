package com.university.marketplace.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.toUserFriendlyMessage
import com.university.marketplace.domain.usecase.GetMyListingsUseCase
import com.university.marketplace.ui.home.ListingUiModel
import com.university.marketplace.ui.home.toUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MyListingsUiState {
    data object Loading : MyListingsUiState
    data object Empty : MyListingsUiState
    data class Success(val listings: List<ListingUiModel>) : MyListingsUiState
    data class Error(val message: String) : MyListingsUiState
}

class MyListingsViewModel(
    private val getMyListingsUseCase: GetMyListingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyListingsUiState>(MyListingsUiState.Loading)
    val uiState: StateFlow<MyListingsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = MyListingsUiState.Loading
            try {
                val listings = getMyListingsUseCase().map { it.toUiModel() }
                _uiState.value = if (listings.isEmpty()) MyListingsUiState.Empty
                else MyListingsUiState.Success(listings)
            } catch (e: Exception) {
                _uiState.value = MyListingsUiState.Error(
                    e.toUserFriendlyMessage(fallback = "Failed to load your listings")
                )
            }
        }
    }
}
