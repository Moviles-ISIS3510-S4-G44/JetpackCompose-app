package com.university.marketplace.ui.home

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.university.marketplace.ui.theme.MarketplaceBackground
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceWhite
import com.university.marketplace.ui.theme.MarketplaceYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMarketplaceScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSell: () -> Unit,
    onNavigateToPurchases: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    isOnline: Boolean,
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()

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
                    onValueChange = {
                        if (isOnline) {
                            viewModel.onSearchQueryChanged(it)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp)),
                    placeholder = { Text(if (isOnline) "Search for items..." else "No internet connection") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MarketplaceWhite,
                        unfocusedContainerColor = MarketplaceWhite,
                        disabledContainerColor = MarketplaceWhite,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    enabled = isOnline,
                    singleLine = true
                )

                if (!isOnline) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You are offline. Search and publishing actions are temporarily disabled.",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Categories
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        val isSelected = selectedCategoryId == category.id
                        SuggestionChip(
                            onClick = { if (isOnline) viewModel.onCategorySelected(category.id) },
                            label = { Text(category.name) },
                            shape = RoundedCornerShape(20.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) MarketplaceYellow else MarketplaceWhite
                            ),
                            border = null
                        )
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
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = { viewModel.loadListings() },
                            colors = ButtonDefaults.buttonColors(containerColor = MarketplaceYellow)
                        ) {
                            Text("Retry", color = MarketplaceDark)
                        }
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
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd),
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
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MarketplaceYellow,
                        modifier = Modifier.size(16.dp)
                    )
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
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MarketplaceYellow,
                            modifier = Modifier.size(14.dp)
                        )
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
            BottomNavItem("Purchases", Icons.Filled.Search, Icons.Outlined.Search, route = "purchase_history"),
            BottomNavItem("Sell", Icons.Filled.Add, Icons.Outlined.Add, route = "create_listing"),
            BottomNavItem("Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, route = "conversations"),
            BottomNavItem("Profile", Icons.Filled.Person, Icons.Outlined.PersonOutline, route = "profile")
        )

        items.forEach { item ->
            val selected = item.route == currentRoute
            NavigationBarItem(
                icon = {
                    if (selected) {
                        Surface(
                            color = MarketplaceYellow.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                item.selectedIcon,
                                contentDescription = item.title,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Icon(item.unselectedIcon, contentDescription = item.title)
                    }
                },
                label = { Text(item.title, fontSize = 10.sp) },
                selected = selected,
                onClick = { onNavigate(item.route) },
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
    val route: String
)
