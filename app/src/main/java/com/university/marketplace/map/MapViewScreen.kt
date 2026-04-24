package com.university.marketplace.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.university.marketplace.ui.common.OfflineBanner
import com.university.marketplace.ui.common.rememberOfflineBannerController
import com.university.marketplace.ui.theme.MarketplaceBackground
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceDarkSecondary
import com.university.marketplace.ui.theme.MarketplaceGray
import com.university.marketplace.ui.theme.MarketplaceWhite
import com.university.marketplace.ui.theme.MarketplaceYellow
import java.util.Locale

private val DEFAULT_MAP_CENTER = LatLng(4.601, -74.065)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapViewScreen(
    isOnline: Boolean,
    productId: String,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: MapViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val offlineBannerController = rememberOfflineBannerController(isOnline)

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val updateUserLocation: () -> Unit = {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLocation = LatLng(location.latitude, location.longitude)
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val hasLocation = it[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            it[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocation) {
            updateUserLocation()
        }
    }

    LaunchedEffect(productId) {
        viewModel.loadListing(productId)

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            updateUserLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val appBarTitle = if (uiState is MapUiState.Success) {
        (uiState as MapUiState.Success).listing.name
    } else {
        "Ubicación"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appBarTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MarketplaceDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MarketplaceWhite,
                    titleContentColor = MarketplaceDark
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MarketplaceBackground)
        ) {
            OfflineBanner(
                isOnline = isOnline,
                offlineBannerController = offlineBannerController
            )

            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is MapUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MarketplaceYellow
                        )
                    }
                    is MapUiState.Error -> {
                        Text(
                            text = state.message,
                            color = Color.Red,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is MapUiState.Success -> {
                        val listing = state.listing
                        val listingLocation = listing.latitude?.let { lat ->
                            listing.longitude?.let { lng -> LatLng(lat, lng) }
                        }
                        val distanceText = userLocation?.let { current ->
                            listingLocation?.let {
                                val distanceKm = DistanceUtils.calculateDistance(
                                    current.latitude,
                                    current.longitude,
                                    it.latitude,
                                    it.longitude
                                )
                                String.format(Locale.US, "Aprox. %.1f km de ti", distanceKm)
                            }
                        } ?: "Ubicación no disponible"

                        val cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(listingLocation ?: DEFAULT_MAP_CENTER, 15f)
                        }

                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(zoomControlsEnabled = false)
                        ) {
                            listingLocation?.let {
                                Marker(
                                    state = rememberMarkerState(position = it),
                                    title = listing.name,
                                    snippet = "$${listing.price.toInt()}"
                                )
                            }

                            userLocation?.let {
                                Marker(
                                    state = rememberMarkerState(position = it),
                                    title = "Tú"
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .fillMaxWidth()
                                .clickable { onNavigateToDetail(listing.id) },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = listing.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = listing.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MarketplaceDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MarketplaceYellow,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = " ${listing.rating}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MarketplaceGray
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "• ${listing.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MarketplaceGray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "$${listing.price.toInt()}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MarketplaceDark
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MarketplaceBackground.copy(alpha = 0.5f))
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = distanceText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MarketplaceDarkSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
