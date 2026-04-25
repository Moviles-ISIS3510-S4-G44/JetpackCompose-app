package com.university.marketplace.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.university.marketplace.R
import com.university.marketplace.domain.Category
import com.university.marketplace.domain.Listing
import com.university.marketplace.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMarketplaceScreen(
    viewModel: HomeViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSell: () -> Unit,
    onNavigateToPurchases: () -> Unit,
    onNavigateToMessages: () -> Unit,
    isOnline: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MarketplacePrimary)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Marketplace",
                            color = MarketplaceWhite,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = MarketplaceWhite)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MarketplacePrimary
                    )
                )
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onSearch = { focusManager.clearFocus() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        bottomBar = {
            MarketplaceBottomNavigation(
                currentRoute = "home",
                onNavigate = { route ->
                    when (route) {
                        "create_listing" -> onNavigateToSell()
                        "purchase_history" -> onNavigateToPurchases()
                        "conversations" -> onNavigateToMessages()
                        "profile" -> onNavigateToProfile()
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            CategoryFilter(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { viewModel.onCategorySelected(it) }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is HomeUiState.Error -> {
                        ErrorMessage(
                            message = state.message,
                            onRetry = { viewModel.loadListings() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is HomeUiState.Success -> {
                        ListingsContent(
                            featured = state.featured,
                            recent = state.recent,
                            isSearching = state.isSearching,
                            onListingClick = onNavigateToDetail,
                            onRefresh = { viewModel.loadListings() }
                        )
                    }
                }
            }
        }
    }

    if (!isOnline) {
        LaunchedEffect(Unit) {
            viewModel.showOfflineState()
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = MarketplaceWhite,
        tonalElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search products...", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) })
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        }
    }
}

@Composable
fun CategoryFilter(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val selected = category.id == selectedCategoryId
            FilterChip(
                selected = selected,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MarketplaceSecondary,
                    selectedLabelColor = MarketplaceWhite
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = Color.LightGray,
                    selectedBorderColor = MarketplaceSecondary
                )
            )
        }
    }
}

@Composable
fun ListingsContent(
    featured: List<ListingUiModel>,
    recent: List<ListingUiModel>,
    isSearching: Boolean,
    onListingClick: (String) -> Unit,
    onRefresh: () -> Unit
) {
    if (featured.isEmpty() && recent.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No results found")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (!isSearching && featured.isNotEmpty()) {
            item {
                SectionTitle("Featured Items")
                FeaturedList(featured, onListingClick)
            }
        }

        if (recent.isNotEmpty()) {
            item {
                SectionTitle(if (isSearching) "Search Results" else "Recent Listings")
            }
            items(recent) { listing ->
                ListingItem(listing, onClick = { onListingClick(listing.id) })
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun FeaturedList(listings: List<ListingUiModel>, onListingClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(listings) { listing ->
            FeaturedItem(listing, onClick = { onListingClick(listing.id) })
        }
    }
}

@Composable
fun FeaturedItem(listing: ListingUiModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
            Column(modifier = Modifier.padding(12.dp)) {
                Text(listing.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$\${listing.price}", color = MarketplaceSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ListingItem(listing: ListingUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
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
        Column(modifier = Modifier.weight(1f)) {
            Text(listing.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(listing.category, color = Color.Gray, fontSize = 14.sp)
            Text("$\${listing.price}", color = MarketplaceSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}

@Composable
fun ErrorMessage(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = Color.Red)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Text("Retry")
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
            BottomNavItem("Purchases", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag, route = "purchase_history"),
            BottomNavItem("Sell", Icons.Filled.Add, Icons.Outlined.Add, route = "create_listing"),
            BottomNavItem("Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, route = "conversations"),
            BottomNavItem("Profile", Icons.Filled.Person, Icons.Outlined.PersonOutline, route = "profile")
        )

        items.forEach { item ->
            val selected = item.route == currentRoute
            NavigationBarItem(
                icon = {
                    Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = item.title)
                },
                label = { Text(item.title) },
                selected = selected,
                onClick = { onNavigate(item.route) }
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
