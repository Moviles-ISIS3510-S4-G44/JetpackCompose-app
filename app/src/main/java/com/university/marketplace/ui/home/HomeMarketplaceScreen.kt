package com.university.marketplace.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.university.marketplace.domain.Category
import com.university.marketplace.ui.common.DistanceLabel
import com.university.marketplace.ui.common.OfflineBanner
import com.university.marketplace.ui.common.rememberOfflineBannerController
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceWhite
import com.university.marketplace.ui.theme.MarketplaceYellow
import androidx.compose.material.icons.filled.LocationOn
import java.util.Locale

@Composable
fun HomeMarketplaceScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSell: () -> Unit,
    onNavigateToPurchases: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    isOnline: Boolean,
    viewModel: HomeViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val selectedPriceCap by viewModel.selectedPriceCap.collectAsState()
    val selectedLocationSort by viewModel.selectedLocationSort.collectAsState()
    val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
    val offlineBannerController = rememberOfflineBannerController(isOnline)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshUserLocation()
    }

    LaunchedEffect(isOnline) {
        if (isOnline) {
            viewModel.loadListings()
        } else {
            viewModel.showOfflineState()
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
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

    Scaffold(
        bottomBar = {
            if (!isLandscape) {
                MarketplaceBottomNavigation(
                    currentRoute = "home",
                    onNavigate = { route ->
                        when (route) {
                            "profile" -> onNavigateToProfile()
                            "create_listing" -> onNavigateToSell()
                            "purchase_history" -> onNavigateToPurchases()
                            "conversations" -> onNavigateToMessages()
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            OfflineBanner(
                isOnline = isOnline,
                offlineBannerController = offlineBannerController,
                message = "Sin conexion. Algunas funciones pueden no estar disponibles."
            )

            SearchHeader(
                searchQuery = searchQuery,
                isOnline = isOnline,
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                selectedPriceCap = selectedPriceCap,
                selectedLocationSort = selectedLocationSort,
                onQueryChanged = viewModel::onSearchQueryChanged,
                onCategorySelected = viewModel::onCategorySelected,
                onPriceSelected = viewModel::onPriceCapSelected,
                onLocationSortSelected = viewModel::onLocationSortSelected,
                onNavigateToFavorites = onNavigateToFavorites
            )

            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MarketplaceYellow)
                    }
                }
                is HomeUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = Color.Red, modifier = Modifier.padding(16.dp))
                    }
                }
                is HomeUiState.Success -> {
                    val onListingClick: (ListingUiModel) -> Unit = { listing ->
                        viewModel.onListingOpened(listing)
                        onNavigateToDetail(listing.id)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (state.isSearching) {
                            item { SectionTitle("Resultados") }
                            val results = state.featured + state.recent
                            if (results.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No se encontraron productos", color = Color.Gray)
                                    }
                                }
                            } else {
                                items(results, key = { it.id }) { listing ->
                                    SearchResultCard(listing = listing, onClick = { onListingClick(listing) })
                                }
                            }
                        } else {
                            item {
                                SectionTitle("Destacados")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(state.featured, key = { it.id }) { listing ->
                                        FeaturedProductCard(listing = listing, onClick = { onListingClick(listing) })
                                    }
                                }
                            }
                            item { SectionTitle("Publicaciones recientes") }
                            items(state.recent, key = { it.id }) { listing ->
                                SearchResultCard(listing = listing, onClick = { onListingClick(listing) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    searchQuery: String,
    isOnline: Boolean,
    categories: List<Category>,
    selectedCategoryId: String?,
    selectedPriceCap: Int?,
    selectedLocationSort: LocationSortOption,
    onQueryChanged: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onPriceSelected: (Int?) -> Unit,
    onLocationSortSelected: (LocationSortOption) -> Unit,
    onNavigateToFavorites: () -> Unit
) {
    val priceOptions = listOf<Int?>(null, 100_000, 300_000, 500_000, 1_000_000)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MarketplaceYellow)
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { if (isOnline) onQueryChanged(it) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp)),
                placeholder = {
                    Text(
                        if (isOnline) "Buscar productos..." else "Sin conexión",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MarketplaceWhite,
                    unfocusedContainerColor = MarketplaceWhite,
                    disabledContainerColor = MarketplaceWhite,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                enabled = isOnline
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onNavigateToFavorites, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Favorite, contentDescription = "Favoritos", tint = MarketplaceDark)
            }
            IconButton(onClick = { }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Notifications, contentDescription = "Notificaciones", tint = MarketplaceDark)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedLocationSort == LocationSortOption.NONE,
                    onClick = { onLocationSortSelected(LocationSortOption.NONE) },
                    label = { Text("Relevancia") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarketplaceWhite,
                        containerColor = Color.White.copy(alpha = 0.65f)
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedLocationSort == LocationSortOption.NEAREST,
                    onClick = { onLocationSortSelected(LocationSortOption.NEAREST) },
                    label = { Text("Más cercanos") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarketplaceWhite,
                        containerColor = Color.White.copy(alpha = 0.65f)
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedLocationSort == LocationSortOption.FARTHEST,
                    onClick = { onLocationSortSelected(LocationSortOption.FARTHEST) },
                    label = { Text("Más lejanos") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarketplaceWhite,
                        containerColor = Color.White.copy(alpha = 0.65f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("Todo") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarketplaceWhite,
                        containerColor = Color.White.copy(alpha = 0.65f)
                    )
                )
            }
            items(categories, key = { it.id }) { category ->
                FilterChip(
                    selected = selectedCategoryId == category.id,
                    onClick = { onCategorySelected(category.id) },
                    label = { Text(category.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarketplaceWhite,
                        containerColor = Color.White.copy(alpha = 0.65f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedLocationSort == LocationSortOption.NONE,
                    onClick = { onLocationSortSelected(LocationSortOption.NONE) },
                    label = { Text("Relevancia") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarketplaceWhite,
                        containerColor = Color.White.copy(alpha = 0.65f)
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedLocationSort == LocationSortOption.NEAREST,
                    onClick = { onLocationSortSelected(LocationSortOption.NEAREST) },
                    label = { Text("Más cercanos") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarketplaceWhite,
                        containerColor = Color.White.copy(alpha = 0.65f)
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedLocationSort == LocationSortOption.FARTHEST,
                    onClick = { onLocationSortSelected(LocationSortOption.FARTHEST) },
                    label = { Text("Más lejanos") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarketplaceWhite,
                        containerColor = Color.White.copy(alpha = 0.65f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(priceOptions, key = { it ?: -1 }) { cap ->
                val label = cap?.let {
                    "Hasta ${String.format(Locale.US, "%,d", it).replace(',', '.')}"
                } ?: "Sin tope"

                FilterChip(
                    selected = selectedPriceCap == cap,
                    onClick = { onPriceSelected(cap) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MarketplaceWhite,
                        containerColor = Color.White.copy(alpha = 0.65f)
                    )
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
fun FeaturedProductCard(listing: ListingUiModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box {
            Column {
                AsyncImage(
                    model = listing.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = listing.name, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "By ${listing.sellerName}", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$ ${String.format(Locale.US, "%,.0f", listing.price).replace(',', '.')}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DistanceLabel(distance = listing.distance)
                }
            }
            Surface(
                modifier = Modifier.padding(8.dp),
                color = MarketplaceYellow,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Destacado",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun SearchResultCard(listing: ListingUiModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = listing.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = listing.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Seller: ${listing.sellerName}", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$ ${String.format(Locale.US, "%,.0f", listing.price).replace(',', '.')}",
                    color = MarketplaceYellow,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                DistanceLabel(distance = listing.distance)
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasCoarseLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return hasFineLocation || hasCoarseLocation
}

@Composable
fun MarketplaceBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MarketplaceWhite,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            BottomNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, route = "home"),
            BottomNavItem("Purchases", Icons.Filled.Search, Icons.Filled.Search, route = "purchase_history"),
            BottomNavItem("Sell", Icons.Filled.Add, Icons.Outlined.Add, route = "create_listing"),
            BottomNavItem("Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, route = "conversations"),
            BottomNavItem("Profile", Icons.Filled.Person, Icons.Outlined.PersonOutline, route = "profile")
        )

        items.forEach { item ->
            val selected = item.route == currentRoute
            NavigationBarItem(
                icon = {
                    Icon(
                        if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title, fontSize = 10.sp) },
                selected = selected,
                onClick = { onNavigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E88E5),
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

data class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)
