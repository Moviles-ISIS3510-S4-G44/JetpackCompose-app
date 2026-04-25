package com.university.marketplace.ui.home

import android.Manifest
import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceWhite
import com.university.marketplace.ui.theme.MarketplaceYellow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMarketplaceScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSell: () -> Unit,
    isOnline: Boolean,
    viewModel: HomeViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories = listOf("Todo", "Electrónica", "Libros", "Muebles", "Accesorios")
    val conditions = listOf("Todos", "Nuevo", "Usado", "Reacondicionado")
    val priceOptions = listOf<Int?>(null, 100_000, 500_000, 1_000_000)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val successState = uiState as? HomeUiState.Success
    val selectedCategory = successState?.selectedCategory ?: "Todo"
    val selectedCondition = successState?.selectedCondition ?: "Todos"
    val selectedPriceCap = successState?.selectedPriceCap
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshUserLocation()
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

    LaunchedEffect(isOnline) {
        val currentState = uiState
        if (isOnline) {
            if (currentState is HomeUiState.Loading || currentState is HomeUiState.Error) {
                viewModel.loadListings()
            }
        } else if (currentState !is HomeUiState.Success) {
            viewModel.showOfflineState()
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
            // Header with Search and Categories (Compact in Landscape)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MarketplaceYellow)
                    .padding(top = if (isLandscape) 4.dp else 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            if (isOnline) {
                                viewModel.onSearchQueryChanged(it)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isLandscape) 44.dp else 52.dp)
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
                    IconButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier.size(if (isLandscape) 32.dp else 48.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notificaciones", tint = MarketplaceDark)
                    }
                }

                if (!isLandscape) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(categories) { category ->
                            CategoryItem(
                                category = category,
                                isSelected = category == selectedCategory,
                                onClick = { viewModel.onCategoryFilterSelected(category) }
                            )
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(conditions) { condition ->
                            FilterChip(
                                selected = selectedCondition == condition,
                                onClick = { viewModel.onConditionFilterSelected(condition) },
                                label = { Text(condition) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MarketplaceWhite,
                                    containerColor = Color.White.copy(alpha = 0.65f)
                                )
                            )
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(priceOptions) { cap ->
                            val label = cap?.let { "Hasta ${String.format(Locale.US, "%,d", it).replace(',', '.')}" } ?: "Sin tope"
                            FilterChip(
                                selected = selectedPriceCap == cap,
                                onClick = { viewModel.onPriceFilterSelected(cap) },
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
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.featured.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SectionHeader(
                                    title = if (state.isSearching) "Search Results" else "Featured",
                                    onSeeAllClick = {}
                                )
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(
                                        items = state.featured,
                                        key = { it.id }
                                    ) { listing ->
                                        FeaturedProductCard(
                                            listing = listing,
                                            onClick = { onNavigateToDetail(listing.id) }
                                        )
                                    }
                                }
                            }

                        if (state.recent.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                SectionHeader(
                                    title = if (state.isSearching) "" else "Recent Listings",
                                    onSeeAllClick = {}
                                )
                            }

                            items(
                                items = state.recent,
                                key = { it.id }
                            ) { listing ->
                                RecentProductCard(
                                    listing = listing,
                                    onClick = { onNavigateToDetail(listing.id) }
                                )
                            }
                        }

                        if (state.featured.isEmpty() && state.recent.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No items found")
                                }
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAllClick: () -> Unit) {
    if (title.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category,
            color = MarketplaceDark,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .background(MarketplaceDark)
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
fun ActivityProductCard(listing: ListingUiModel, onClick: () -> Unit) {
    Column(modifier = Modifier.width(160.dp)) {
        DistanceLabel(distance = listing.distance)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
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
                    Text(text = listing.name, fontSize = 14.sp, maxLines = 2, minLines = 2)
                    Text(text = "$ ${String.format(Locale.US, "%,.0f", listing.price).replace(',', '.')}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun FeaturedProductCard(listing: ListingUiModel, onClick: () -> Unit) {
    Column(modifier = Modifier.width(160.dp)) {
        DistanceLabel(distance = listing.distance)
        Card(
            modifier = Modifier
                .fillMaxWidth()
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
                        Text(text = listing.name, fontSize = 14.sp, maxLines = 2, minLines = 2)
                        Text(text = "$ ${String.format(Locale.US, "%,.0f", listing.price).replace(',', '.')}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RecentProductCard(listing: ListingUiModel, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier) {
        DistanceLabel(distance = listing.distance)
        Card(
            modifier = Modifier.clickable { onClick() },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column {
                AsyncImage(
                    model = listing.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = listing.name, fontSize = 14.sp, maxLines = 1)
                    Text(text = "$ ${String.format(Locale.US, "%,.0f", listing.price).replace(',', '.')}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(listing: ListingUiModel, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        DistanceLabel(distance = listing.distance)
        Card(
            modifier = Modifier
                .fillMaxWidth()
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
                    Text(text = listing.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "$ ${String.format(Locale.US, "%,.0f", listing.price).replace(',', '.')}", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun DistanceLabel(distance: String?) {
    Text(
        text = distance ?: "Aprox. distancia no disponible",
        fontSize = 12.sp,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 6.dp)
    )
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
            BottomNavItem("Inicio", Icons.Filled.Home, Icons.Outlined.Home, route = "home"),
            BottomNavItem("Vender", Icons.Filled.Add, Icons.Outlined.Add, route = "create_listing"),
            BottomNavItem("Carrito", Icons.Filled.Add, Icons.Outlined.Add, route = "home"),
            BottomNavItem("Mensajes", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, route = "home"),
            BottomNavItem("Perfil", Icons.Filled.Person, Icons.Outlined.PersonOutline, route = "profile")
        )

        items.forEach { item ->
            val selected = item.route == currentRoute
            NavigationBarItem(
                icon = {
                    if (item.title == "Carrito") {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White, RoundedCornerShape(25.dp))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = item.title)
                    }
                },
                label = { if (item.title != "Carrito") Text(item.title, fontSize = 10.sp) },
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

private fun hasLocationPermission(context: android.content.Context): Boolean {
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
