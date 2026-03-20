package com.university.marketplace.map

import com.university.marketplace.domain.Product

data class MapUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val hasLocationPermission: Boolean = false,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val distanceLabel: String = "Activa tu ubicacion para calcular distancia",
    val errorMessage: String? = null
)

