package com.university.marketplace.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.ProductRepository
import com.university.marketplace.data.location.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

class MapViewModel(
    private val productId: String,
    private val productRepository: ProductRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadProduct()
    }

    private fun loadProduct() {
        val product = productRepository.getProductById(productId)
        if (product == null) {
            _uiState.value = MapUiState(
                isLoading = false,
                errorMessage = "No se encontro el producto"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            product = product,
            errorMessage = null
        )
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasLocationPermission = granted,
            distanceLabel = if (granted) "Calculando distancia..." else "Activa tu ubicacion para calcular distancia"
        )

        if (granted) {
            fetchUserLocation()
        }
    }

    fun fetchUserLocation() {
        val product = _uiState.value.product ?: return
        if (!_uiState.value.hasLocationPermission) return

        viewModelScope.launch {
            val userLocation = locationRepository.getLastKnownLocation()
            if (userLocation == null) {
                _uiState.value = _uiState.value.copy(
                    userLatitude = null,
                    userLongitude = null,
                    distanceLabel = "No se pudo obtener tu ubicacion actual"
                )
                return@launch
            }

            val distanceKm = DistanceUtils.calculateDistance(
                lat1 = userLocation.latitude,
                lon1 = userLocation.longitude,
                lat2 = product.latitude,
                lon2 = product.longitude
            )

            _uiState.value = _uiState.value.copy(
                userLatitude = userLocation.latitude,
                userLongitude = userLocation.longitude,
                distanceLabel = formatDistance(distanceKm)
            )
        }
    }

    private fun formatDistance(distanceKm: Double): String {
        return if (distanceKm < 1) {
            val meters = (distanceKm * 1000).roundToInt()
            "Aprox. ${meters} m de ti"
        } else {
            val kmRounded = String.format(Locale.US, "%.1f", distanceKm)
            "Aprox. ${kmRounded} km de ti"
        }
    }
}

class MapViewModelFactory(
    private val productId: String,
    private val productRepository: ProductRepository,
    private val locationRepository: LocationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(productId, productRepository, locationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

