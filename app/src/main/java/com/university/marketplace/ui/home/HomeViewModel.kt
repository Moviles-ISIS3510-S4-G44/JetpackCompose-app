package com.university.marketplace.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.university.marketplace.data.ProductRepository
import com.university.marketplace.domain.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        _products.value = repository.getProducts()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _products.value = repository.searchProducts(query)
        // Analytics: track search event
        println("Analytics: User searched for $query")
    }
}

@Suppress("unused")
class HomeViewModelFactory(
    private val repository: ProductRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

