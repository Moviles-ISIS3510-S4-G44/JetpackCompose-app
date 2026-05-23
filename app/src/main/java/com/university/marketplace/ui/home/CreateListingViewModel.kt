package com.university.marketplace.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.NotificationRepository
import com.university.marketplace.data.auth.AuthRepository
import com.university.marketplace.data.toUserFriendlyMessage
import com.university.marketplace.domain.Category
import com.university.marketplace.domain.CategoryRepository
import com.university.marketplace.domain.Listing
import com.university.marketplace.domain.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CreateListingUiState {
    data object Idle : CreateListingUiState
    data object Loading : CreateListingUiState
    data class Success(val listing: Listing) : CreateListingUiState
    data class Error(val message: String) : CreateListingUiState
}

class CreateListingViewModel(
    private val listingRepository: ListingRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateListingUiState>(CreateListingUiState.Idle)
    val uiState: StateFlow<CreateListingUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            runCatching {
                currentUserId = authRepository.getCurrentUser().id
                _categories.value = categoryRepository.getCategories()
            }
        }
    }

    fun submit(
        categoryId: String,
        title: String,
        description: String,
        price: Int,
        condition: String,
        images: List<String>,
        location: String
    ) {
        val sellerId = currentUserId ?: run {
            _uiState.value = CreateListingUiState.Error("Not signed in")
            return
        }
        viewModelScope.launch {
            _uiState.value = CreateListingUiState.Loading
            try {
                val listing = listingRepository.createListing(
                    sellerId = sellerId,
                    categoryId = categoryId,
                    title = title,
                    description = description,
                    price = price,
                    condition = condition,
                    images = images,
                    location = location
                )
                

                notificationRepository.insertNotification(
                    message = "Has publicado con éxito: ${listing.title}",
                    type = "SYSTEM"
                )

                _uiState.value = CreateListingUiState.Success(listing)
            } catch (e: Exception) {
                _uiState.value = CreateListingUiState.Error(
                    e.toUserFriendlyMessage(fallback = "Failed to create listing")
                )
            }
        }
    }

    fun resetError() {
        if (_uiState.value is CreateListingUiState.Error) {
            _uiState.value = CreateListingUiState.Idle
        }
    }
}
