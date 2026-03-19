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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.university.marketplace.domain.Product
import com.university.marketplace.ui.theme.MarketplaceYellow
import com.university.marketplace.ui.theme.MarketplaceBackground
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMarketplaceScreen(
    onNavigateToMap: (Product) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching = searchQuery.isNotBlank()
    val categories = listOf("Books", "Electronics", "Furniture", "Study")

    Scaffold(
        bottomBar = { MarketplaceBottomNavigation() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MarketplaceBackground)
        ) {
            item {
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
                }
            }

            if (isSearching) {
                item {
                    SectionHeader(title = "Resultados", onSeeAllClick = {})
                }

                if (products.isEmpty()) {
                    item {
                        Text(
                            text = "No encontramos productos para '$searchQuery'",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    items(products.chunked(2)) { pair ->
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            pair.forEach { product ->
                                RecentProductCard(
                                    product = product,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToMap(product) }
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // Featured Section
                item {
                    SectionHeader(title = "Featured", onSeeAllClick = {})
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(products.filter { it.isFeatured }) { product ->
                            FeaturedProductCard(product, onClick = { onNavigateToMap(product) })
                        }
                    }
                }

                // Recent Listings Section
                item {
                    SectionHeader(title = "Recent Listings", onSeeAllClick = {})
                }

                items(products.filter { !it.isFeatured }.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        pair.forEach { product ->
                            RecentProductCard(
                                product = product,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToMap(product) }
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAllClick: () -> Unit) {
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
fun FeaturedProductCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite)
    ) {
        Box {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
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
            Text(text = product.name, maxLines = 1, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "$${product.price.toInt()}", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MarketplaceYellow, modifier = Modifier.size(16.dp))
                    Text(text = "${product.rating}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun RecentProductCard(product: Product, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite)
    ) {
        Column {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = product.name, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "$${product.price.toInt()}", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MarketplaceYellow, modifier = Modifier.size(14.dp))
                        Text(text = "${product.rating}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun MarketplaceBottomNavigation() {
    NavigationBar(
        containerColor = MarketplaceWhite,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            BottomNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem("Search", Icons.Filled.Search, Icons.Outlined.Search),
            BottomNavItem("Sell", Icons.Filled.Add, Icons.Outlined.Add),
            BottomNavItem("Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
            BottomNavItem("Profile", Icons.Filled.Person, Icons.Outlined.PersonOutline)
        )
        
        var selectedItem by remember { mutableStateOf(0) }

        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { 
                    if (selectedItem == index) {
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
                selected = selectedItem == index,
                onClick = { selectedItem = index },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MarketplaceDark,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

data class BottomNavItem(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)
