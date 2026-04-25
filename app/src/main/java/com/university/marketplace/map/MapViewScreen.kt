package com.university.marketplace.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SignalWifiOff
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.university.marketplace.ui.common.OfflineBanner
import com.university.marketplace.ui.common.isWideScreen
import com.university.marketplace.ui.common.rememberOfflineBannerController
import com.university.marketplace.ui.theme.MarketplaceBackground
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceDarkSecondary
import com.university.marketplace.ui.theme.MarketplaceGray
import com.university.marketplace.ui.theme.MarketplaceWhite
import com.university.marketplace.ui.theme.MarketplaceYellow
import java.util.Locale

private val DEFAULT_MAP_CENTER = LatLng(4.601, -74.065)

private val LatLngSaver = listSaver<LatLng?, Double>(
    save = { value -> value?.let { listOf(it.latitude, it.longitude) } ?: emptyList() },
    restore = { list -> if (list.size == 2) LatLng(list[0], list[1]) else null }
)

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
    var userLocation by rememberSaveable(stateSaver = LatLngSaver) {
        mutableStateOf<LatLng?>(null)
    }

    val updateUserLocation: () -> Unit = {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        userLocation = LatLng(loc.latitude, loc.longitude)
                    }
                }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        updateUserLocation()
    }

    LaunchedEffect(productId, isOnline) {
        val currentState = uiState
        if (currentState is MapUiState.Success) return@LaunchedEffect
        if (!isOnline) {
            viewModel.showOfflineState()
            return@LaunchedEffect
        }
        viewModel.resetToLoading()
        viewModel.loadListing(productId)
    }

    LaunchedEffect(Unit) {
        if (userLocation != null) return@LaunchedEffect
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
        "Listing Location"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appBarTitle, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
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
                        val listingLocation = remember(listing.latitude, listing.longitude) {
                            listing.latitude?.let { lat ->
                                listing.longitude?.let { lng -> LatLng(lat, lng) }
                            }
                        }
                        val distanceText = remember(userLocation, listingLocation) {
                            userLocation?.let { current ->
                                listingLocation?.let {
                                    val distanceKm = DistanceUtils.calculateDistance(
                                        current.latitude,
                                        current.longitude,
                                        it.latitude,
                                        it.longitude
                                    )
                                    String.format(Locale.US, "Approx. %.1f km from you", distanceKm)
                                }
                            } ?: "Location unavailable"
                        }
                        var isMapLoaded by remember(listing.id) { mutableStateOf(false) }

                        val cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(
                                listingLocation ?: DEFAULT_MAP_CENTER,
                                15f
                            )
                        }

                        if (isWideScreen()) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    MapContent(
                                        cameraPositionState = cameraPositionState,
                                        listingLocation = listingLocation,
                                        userLocation = userLocation,
                                        listing = listing,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .widthIn(min = 280.dp, max = 360.dp)
                                        .fillMaxHeight()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ListingSummaryCard(
                                        listing = listing,
                                        distanceText = distanceText,
                                        onClick = { onNavigateToDetail(listing.id) }
                                    )
                                }
                            }
                        } else {
                            MapContent(
                                cameraPositionState = cameraPositionState,
                                listingLocation = listingLocation,
                                userLocation = userLocation,
                                listing = listing,
                                modifier = Modifier.fillMaxSize()
                            )

                            BottomCardWrapper {
                                ListingSummaryCard(
                                    listing = listing,
                                    distanceText = distanceText,
                                    onClick = { onNavigateToDetail(listing.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.BottomCardWrapper(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun MapContent(
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    listingLocation: LatLng?,
    userLocation: LatLng?,
    listing: com.university.marketplace.ui.home.ListingUiModel,
    modifier: Modifier = Modifier
) {
    GoogleMap(
        modifier = modifier,
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
                title = "You"
            )
        }
    }
}

@Composable
private fun ListingSummaryCard(
    listing: com.university.marketplace.ui.home.ListingUiModel,
    distanceText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
