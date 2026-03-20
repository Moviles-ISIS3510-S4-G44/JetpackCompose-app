package com.university.marketplace.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.ListingsRepository
import com.university.marketplace.data.api.ListingDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ListingsRepository = ListingsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allListings: List<ListingUiModel> = emptyList()

    init {
        loadListings()
    }

    fun loadListings() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val dtos = repository.getListings()
                val activeListings = dtos
                    .filter { it.status.equals("active", ignoreCase = true) }
                    .map { it.toUiModel() }
                
                allListings = activeListings
                updateSections(activeListings)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    private fun updateSections(listings: List<ListingUiModel>) {
        val featured = listings.take(4)
        val recent = listings.drop(4)
        _uiState.value = HomeUiState.Success(
            featured = featured,
            recent = recent,
            isSearching = _searchQuery.value.isNotEmpty()
        )
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        val filtered = if (query.isEmpty()) {
            allListings
        } else {
            allListings.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true)
            }
        }
        updateSections(filtered)
    }

    private fun ListingDto.toUiModel() = ListingUiModel(
        id = id,
        name = title,
        price = price,
        imageUrl = images.firstOrNull() ?: "",
        description = description
    )
}
