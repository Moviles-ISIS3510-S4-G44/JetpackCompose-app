package com.university.marketplace.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.InteractionsRepository
import com.university.marketplace.data.toUserFriendlyMessage
import com.university.marketplace.domain.FavoriteRepository
import com.university.marketplace.domain.usecase.CreatePurchaseUseCase
import com.university.marketplace.domain.usecase.GetListingByIdUseCase
import com.university.marketplace.ui.common.toUserFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListingDetailUiState(
    val content: Content = Content.Loading,
    val isFavorited: Boolean = false
) {
    sealed interface Content {
        object Loading : Content
        data class Success(val listing: ListingUiModel) : Content
        data class Error(val message: String) : Content
    }
}

sealed interface PurchaseUiState {
    data object Idle : PurchaseUiState
    data object Loading : PurchaseUiState
    data object Success : PurchaseUiState
    data class Error(val message: String) : PurchaseUiState
}

class ListingDetailViewModel(
    private val getListingByIdUseCase: GetListingByIdUseCase,
    private val interactionsRepository: InteractionsRepository,
    private val favoriteRepository: FavoriteRepository,
    private val createPurchaseUseCase: CreatePurchaseUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListingDetailUiState())
    val uiState: StateFlow<ListingDetailUiState> = _uiState.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseUiState>(PurchaseUiState.Idle)
    val purchaseState: StateFlow<PurchaseUiState> = _purchaseState.asStateFlow()

    fun loadListing(id: String) {
        favoriteRepository.isFavorite(id)
            .onEach { isFav ->
                _uiState.update { it.copy(isFavorited = isFav) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            _uiState.update { it.copy(content = ListingDetailUiState.Content.Loading) }
            try {
                val listing = getListingByIdUseCase(id)
                _uiState.update { it.copy(content = ListingDetailUiState.Content.Success(listing.toUiModel())) }
                viewModelScope.launch {
                    interactionsRepository.registerVisit(listing.id)
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(content = ListingDetailUiState.Content.Error(
                        e.toUserFriendlyMessage(fallback = "Failed to load listing")
                    ))
                }
            }
        }
    }

    fun showOfflineState() {
        if (_uiState.value.content is ListingDetailUiState.Content.Success) return
        _uiState.update { 
            it.copy(content = ListingDetailUiState.Content.Error(
                "You appear to be offline. Please check your connection and try again."
            ))
        }
    }

    fun resetToLoading() {
        if (_uiState.value.content is ListingDetailUiState.Content.Success) return
        _uiState.update { it.copy(content = ListingDetailUiState.Content.Loading) }
    }

    fun purchase(listingId: String) {
        val useCase = createPurchaseUseCase ?: return
        viewModelScope.launch {
            _purchaseState.value = PurchaseUiState.Loading
            try {
                useCase(listingId)
                _purchaseState.value = PurchaseUiState.Success
            } catch (e: Exception) {
                _purchaseState.value = PurchaseUiState.Error(
                    e.toUserFriendlyMessage(fallback = "Purchase failed")
                )
            }
        }
    }

    fun toggleFavorite(listingId: String) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(listingId)
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseUiState.Idle
    }
}
