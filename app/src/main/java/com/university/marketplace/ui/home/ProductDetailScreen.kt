package com.university.marketplace.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.university.marketplace.map.DistanceUtils
import com.university.marketplace.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    viewModel: ListingDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val updateLocation: () -> Unit = {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    userCoordinates = location.latitude to location.longitude
                }
            }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val hasLocation = it[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            it[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocation) {
            updateLocation()
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
            updateLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del producto", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Compartir */ }) { Icon(Icons.Outlined.Share, null) }
                    IconButton(onClick = { /* Favoritos */ }) { Icon(Icons.Outlined.FavoriteBorder, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (uiState is ListingDetailUiState.Success) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { /* Contactar */ },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MarketplaceYellow),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = MarketplaceDark)
                            Spacer(Modifier.width(8.dp))
                            Text("Contactar vendedor", color = MarketplaceDark, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MarketplaceWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            IconButton(onClick = { /* Guardar */ }) {
                                Icon(Icons.Default.BookmarkBorder, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is ListingDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MarketplaceYellow)
                }
                is ListingDetailUiState.Error -> {
                    Text(text = state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                }
                is ListingDetailUiState.Success -> {
                    val listing = state.listing
                    val distanceText = if (listing.latitude != null && listing.longitude != null) {
                        userCoordinates?.let { (lat, lon) ->
                            val distanceKm = DistanceUtils.calculateDistance(
                                lat,
                                lon,
                                listing.latitude,
                                listing.longitude
                            )
                            String.format(Locale.US, "%s • a %.1f km", listing.locationName, distanceKm)
                        } ?: "${listing.locationName} • distancia no disponible"
                    } else {
                        "${listing.locationName} • ubicación no disponible"
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .verticalScroll(rememberScrollState())
                    ) {
                        AsyncImage(
                            model = listing.imageUrl,
                            contentDescription = listing.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp),
                            contentScale = ContentScale.Crop
                        )

                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = listing.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = Color(0xFFF5F5F5),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = listing.condition.uppercase(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "$${listing.price.toInt()}",
                                color = MarketplaceYellow,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                thickness = DividerDefaults.Thickness,
                                color = Color.LightGray.copy(alpha = 0.5f)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.LightGray))
                                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                    Text(listing.sellerName, fontWeight = FontWeight.Bold)
                                    Text("Estudiante universitario", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text("Ver perfil", color = MarketplaceYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text("Descripción", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = listing.description,
                                modifier = Modifier.padding(top = 8.dp),
                                color = Color.DarkGray,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                Text(
                                    text = distanceText,
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
