package com.university.marketplace.ui.home

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.runtime.saveable.listSaver
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
import com.university.marketplace.ui.common.DistanceLabel
import com.university.marketplace.ui.common.isWideScreen
import com.university.marketplace.ui.theme.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    isOnline: Boolean,
    onBack: () -> Unit,
    onMessageSeller: (() -> Unit)? = null,
    onNavigateToSellerProfile: (String, String) -> Unit,
    viewModel: ListingDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val purchaseState by viewModel.purchaseState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            viewModel.refreshUserLocation()
        }
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
        val currentState = uiState.content
        if (currentState is ListingDetailUiState.Content.Success) return@LaunchedEffect
        if (!isOnline) {
            viewModel.showOfflineState()
            return@LaunchedEffect
        }
        viewModel.resetToLoading()
        viewModel.loadListing(productId)
    }

    LaunchedEffect(Unit) {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            viewModel.refreshUserLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    if (showPurchaseDialog) {
        val listing = (uiState.content as? ListingDetailUiState.Content.Success)?.listing
        AlertDialog(
            onDismissRequest = { showPurchaseDialog = false },
            title = { Text("Confirm Purchase") },
            text = {
                Text("Buy \"\${listing?.name ?: \"this item\"}\" for $\${listing?.price?.toInt()}?")
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
                title = { Text("Detalle del producto", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Compartir */ }) { Icon(Icons.Outlined.Share, null) }
                    IconButton(onClick = { viewModel.toggleFavorite(productId) }) {
                        Icon(
                            imageVector = if (uiState.isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (uiState.isFavorited) Color.Red else LocalContentColor.current
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (uiState.content is ListingDetailUiState.Content.Success) {
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

                        if (onMessageSeller != null) {
                            Button(
                                onClick = { if (isOnline) onMessageSeller() },
                                modifier = Modifier.weight(1f).height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MarketplaceWhite),
                                shape = RoundedCornerShape(28.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MarketplaceYellow),
                                enabled = isOnline
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = MarketplaceDark)
                                Spacer(Modifier.width(8.dp))
                                Text("Message", color = MarketplaceDark, fontWeight = FontWeight.Bold)
                            }
                        } else {
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
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState.content) {
                is ListingDetailUiState.Content.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MarketplaceYellow)
                }
                is ListingDetailUiState.Content.Error -> {
                    Text(text = state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                }
                is ListingDetailUiState.Content.Success -> {
                    val listing = state.listing
                    if (isWideScreen()) {
                        WideListingDetail(
                            listing = listing,
                            onNavigateToSellerProfile = onNavigateToSellerProfile
                        )
                    } else {
                        CompactListingDetail(
                            listing = listing,
                            onNavigateToSellerProfile = onNavigateToSellerProfile
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
    onNavigateToSellerProfile: (String, String) -> Unit
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
        ListingDetailBody(listing = listing, onNavigateToSellerProfile = onNavigateToSellerProfile)
    }
}

@Composable
private fun WideListingDetail(
    listing: ListingUiModel,
    onNavigateToSellerProfile: (String, String) -> Unit
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
            ListingDetailBody(listing = listing, onNavigateToSellerProfile = onNavigateToSellerProfile)
        }
    }
}

@Composable
private fun ListingDetailBody(
    listing: ListingUiModel,
    onNavigateToSellerProfile: (String, String) -> Unit
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
            modifier = Modifier.padding(top = 8.dp)
        )

        DistanceLabel(
            distance = listing.distance,
            modifier = Modifier.padding(bottom = 8.dp),
            iconSize = 16.dp,
            fontSize = 14.sp
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
            TextButton(onClick = { onNavigateToSellerProfile(listing.sellerId, listing.sellerName) }) {
                Text("View Profile", color = MarketplaceYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Description", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(
            text = listing.description,
            modifier = Modifier.padding(top = 8.dp),
            color = Color.DarkGray,
            lineHeight = 22.sp
        )
    }
}
