package com.university.marketplace.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.university.marketplace.domain.Product
import com.university.marketplace.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapViewScreen(
    product: Product,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val productLocation = LatLng(product.latitude, product.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(productLocation, 15f)
    }
    val markerState = rememberMarkerState(position = productLocation)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var requestedLocationPermission by rememberSaveable { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.fetchLastKnownLocation { location ->
                userLocation = location
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission && !requestedLocationPermission) {
            requestedLocationPermission = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val distanceText = when {
        !hasLocationPermission -> "Activa tu ubicacion para calcular distancia"
        userLocation == null -> "No se pudo obtener tu ubicacion actual"
        else -> {
            val distanceKm = DistanceUtils.calculateDistance(
                lat1 = userLocation!!.latitude,
                lon1 = userLocation!!.longitude,
                lat2 = product.latitude,
                lon2 = product.longitude
            )
            formatDistance(distanceKm)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Regresar",
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
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MarketplaceBackground)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
            ) {
                Marker(
                    state = markerState,
                    title = product.name,
                    snippet = "${product.locationLabel} - $${product.price.toInt()}"
                )

                userLocation?.let { currentLocation ->
                    Marker(
                        state = rememberMarkerState(position = currentLocation),
                        title = "Tu ubicacion"
                    )
                }
            }

            // Product Detail Card on Map
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
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
                                text = " ${product.rating}", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MarketplaceGray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${product.category}", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MarketplaceGray
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = MarketplaceGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = " ${product.locationLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MarketplaceGray
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$${product.price.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MarketplaceDark
                        )
                    }
                }
                
                // Distance Indicator
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

@SuppressLint("MissingPermission")
private fun com.google.android.gms.location.FusedLocationProviderClient.fetchLastKnownLocation(
    onResult: (LatLng?) -> Unit
) {
    lastLocation
        .addOnSuccessListener { location ->
            onResult(location?.let { LatLng(it.latitude, it.longitude) })
        }
        .addOnFailureListener {
            onResult(null)
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
