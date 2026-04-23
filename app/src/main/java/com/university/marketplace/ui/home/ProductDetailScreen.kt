package com.university.marketplace.ui.home

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.university.marketplace.ui.common.isWideScreen
import com.university.marketplace.ui.theme.*
import java.util.Locale

private val UserCoordinatesSaver = listSaver<Pair<Double, Double>?, Double>(
    save = { value -> value?.let { listOf(it.first, it.second) } ?: emptyList() },
    restore = { list -> if (list.size == 2) list[0] to list[1] else null }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    isOnline: Boolean,
    onBack: () -> Unit,
    viewModel: ListingDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val purchaseState by viewModel.purchaseState.collectAsState()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userCoordinates by rememberSaveable(stateSaver = UserCoordinatesSaver) {
        mutableStateOf<Pair<Double, Double>?>(null)
    }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(purchaseState) {
        when (val ps = purchaseState) {
            is PurchaseUiState.Success -> {
                snackbarHostState.showSnackbar("Purchase successful!")
                viewModel.resetPurchaseState()
            }
            is PurchaseUiState.Error -> {
                snackbarHostState.showSnackbar(ps.message)
                viewModel.resetPurchaseState()
            }
            else -> {}
        }
    }

    LaunchedEffect(productId, isOnline) {
        val currentState = uiState
        if (currentState is ListingDetailUiState.Success) return@LaunchedEffect
        if (!isOnline) {
            viewModel.showOfflineState()
            return@LaunchedEffect
        }
        viewModel.resetToLoading()
        viewModel.loadListing(productId)
    }

    LaunchedEffect(Unit) {
        if (userCoordinates != null) return@LaunchedEffect
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userCoordinates = location.latitude to location.longitude
                    }
                }
        }
    }

    if (showPurchaseDialog) {
        val listing = (uiState as? ListingDetailUiState.Success)?.listing
        AlertDialog(
            onDismissRequest = { showPurchaseDialog = false },
            title = { Text("Confirm Purchase") },
            text = {
                Text("Buy \"${listing?.name ?: "this item"}\" for $${listing?.price?.toInt()}?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showPurchaseDialog = false
                    listing?.let { viewModel.purchase(it.id) }
                }) { Text("Buy") }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Product Details", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        val isPurchasing = purchaseState is PurchaseUiState.Loading
                        Button(
                            onClick = { if (isOnline && !isPurchasing) showPurchaseDialog = true },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MarketplaceYellow),
                            shape = RoundedCornerShape(28.dp),
                            enabled = isOnline && !isPurchasing
                        ) {
                            if (isPurchasing) {
                                CircularProgressIndicator(
                                    color = MarketplaceDark,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = MarketplaceDark)
                                Spacer(Modifier.width(8.dp))
                                Text("Buy Now", color = MarketplaceDark, fontWeight = FontWeight.Bold)
                            }
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
                    val distanceText = remember(listing, userCoordinates) {
                        if (listing.latitude != null && listing.longitude != null) {
                            userCoordinates?.let { (lat, lon) ->
                                val distanceKm = DistanceUtils.calculateDistance(
                                    lat,
                                    lon,
                                    listing.latitude,
                                    listing.longitude
                                )
                                String.format(Locale.US, "%s • %.1f km away", listing.locationName, distanceKm)
                            } ?: "${listing.locationName} • distance unavailable"
                        } else {
                            "${listing.locationName} • location unavailable"
                        }
                    }
                    if (isWideScreen()) {
                        WideListingDetail(
                            listing = listing,
                            distanceText = distanceText
                        )
                    } else {
                        CompactListingDetail(
                            listing = listing,
                            distanceText = distanceText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactListingDetail(
    listing: ListingUiModel,
    distanceText: String
) {
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
        ListingDetailBody(listing = listing, distanceText = distanceText)
    }
}

@Composable
private fun WideListingDetail(
    listing: ListingUiModel,
    distanceText: String
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AsyncImage(
            model = listing.imageUrl,
            contentDescription = listing.name,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            ListingDetailBody(listing = listing, distanceText = distanceText)
        }
    }
}

@Composable
private fun ListingDetailBody(
    listing: ListingUiModel,
    distanceText: String
) {
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
                Text("University Student", fontSize = 12.sp, color = Color.Gray)
            }
            Text("View Profile", color = MarketplaceYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Description", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
