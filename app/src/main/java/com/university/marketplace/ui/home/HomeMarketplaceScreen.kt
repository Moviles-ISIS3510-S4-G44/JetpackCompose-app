package com.university.marketplace.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.university.marketplace.ui.theme.MarketplaceYellow
import com.university.marketplace.ui.theme.MarketplaceBackground
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMarketplaceScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToMap: (Product) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSell: () -> Unit,
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching = searchQuery.isNotBlank()
    val categories = listOf("Books", "Electronics", "Furniture", "Study")

    Scaffold(
        bottomBar = {
            MarketplaceBottomNavigation(
                currentRoute = "home",
                onNavigate = { route ->
                    if (route == "profile") {
                        onNavigateToProfile()
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MarketplaceBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "University Marketplace",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MarketplaceDark
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp)),
                    placeholder = { Text("Search for items...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MarketplaceWhite,
                        unfocusedContainerColor = MarketplaceWhite,
                        disabledContainerColor = MarketplaceWhite,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Categories
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(category) },
                            shape = RoundedCornerShape(20.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MarketplaceWhite
                            ),
                            border = null
                        )
                    }
                }

            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MarketplaceYellow)
                    }
                }
                is HomeUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = Color.Red, modifier = Modifier.padding(16.dp))
                        Button(
                            onClick = { viewModel.loadListings() },
                            colors = ButtonDefaults.buttonColors(containerColor = MarketplaceYellow)
                        ) {
                            Text("Retry", color = MarketplaceDark)
                        }
                    }
                }
                is HomeUiState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // Featured Section
                        if (state.featured.isNotEmpty()) {
                            item {
                                SectionHeader(title = if (state.isSearching) "Search Results" else "Featured", onSeeAllClick = {})
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(state.featured) { listing ->
                                        FeaturedProductCard(listing, onClick = { onNavigateToDetail(listing.id) })
                                    }
                                }
                            }
                        }

                        // Recent Listings Section
                        if (state.recent.isNotEmpty()) {
                            item {
                                SectionHeader(title = if (state.isSearching) "" else "Recent Listings", onSeeAllClick = {})
                            }

                            items(state.recent.chunked(2)) { pair ->
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    pair.forEach { listing ->
                                        RecentProductCard(
                                            listing = listing,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onNavigateToDetail(listing.id) }
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        
                        if (state.featured.isEmpty() && state.recent.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No items found")
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
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
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = "See all",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.clickable { onSeeAllClick() }
        )
    }
}

@Composable
fun FeaturedProductCard(listing: ListingUiModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite)
    ) {
        Box {
            AsyncImage(
                model = listing.imageUrl,
                contentDescription = listing.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentScale = ContentScale.Crop
            )
            // Featured Badge
            Surface(
                modifier = Modifier.padding(8.dp).align(Alignment.TopEnd),
                color = MarketplaceYellow,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Featured",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = listing.name, maxLines = 1, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "$${listing.price.toInt()}", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MarketplaceYellow, modifier = Modifier.size(16.dp))
                    Text(text = "${listing.rating}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun RecentProductCard(listing: ListingUiModel, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite)
    ) {
        Column {
            AsyncImage(
                model = listing.imageUrl,
                contentDescription = listing.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = listing.name, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "$${listing.price.toInt()}", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MarketplaceYellow, modifier = Modifier.size(14.dp))
                        Text(text = "${listing.rating}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
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
            BottomNavItem("Search", Icons.Filled.Search, Icons.Outlined.Search),
            BottomNavItem("Sell", Icons.Filled.Add, Icons.Outlined.Add),
            BottomNavItem("Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
            BottomNavItem("Profile", Icons.Filled.Person, Icons.Outlined.PersonOutline, route = "profile")
        )

        var selectedItem by remember { mutableStateOf(0) }

        items.forEach { item ->
            val selected = item.route == currentRoute
            NavigationBarItem(
                icon = { 
                    if (selected) {
                        Surface(
                            color = MarketplaceYellow.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(item.selectedIcon, contentDescription = item.title, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                        }
                    } else {
                        Icon(item.unselectedIcon, contentDescription = item.title)
                    }
                },
                label = { Text(item.title, fontSize = 10.sp) },
                selected = selected,
                onClick = {
                    item.route?.let(onNavigate)
                },
                selected = selectedItem == index,
                onClick = { selectedItem = index
                          if (item.title == "Sell") {
                              onSellClick()
                          }

                          },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MarketplaceDark,
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
    val route: String? = null
)
