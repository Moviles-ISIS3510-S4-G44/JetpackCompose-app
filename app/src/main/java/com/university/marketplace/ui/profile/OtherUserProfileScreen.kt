package com.university.marketplace.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.university.marketplace.domain.AuthenticatedUser
import com.university.marketplace.ui.home.ListingUiModel
import com.university.marketplace.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    sellerId: String,
    initialSellerName: String? = null,
    viewModel: OtherUserProfileViewModel,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sellerId) {
        viewModel.loadSellerProfile(sellerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seller Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MarketplaceDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(MarketplaceBackground)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MarketplaceYellow
                    )
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage!!,
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center).padding(20.dp)
                    )
                }
                else -> {
                    // Always use the data from the server as the source of truth
                    val fetchedUser = uiState.user
                    
                    val displayUser = if (fetchedUser != null) {
                        fetchedUser
                    } else {
                        // Fallback while loading or if not found
                        val rawName = if (!initialSellerName.isNullOrBlank()) initialSellerName else sellerId
                        val cleanName = if (rawName == sellerId && (rawName.contains("-") || rawName.length > 20)) {
                            formatSellerId(rawName)
                        } else {
                            rawName
                        }
                        AuthenticatedUser(sellerId, cleanName, "", 4)
                    }

                    SellerProfileContent(
                        user = displayUser,
                        listings = uiState.listings,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
            }
        }
    }
}

private fun formatSellerId(id: String): String {
    return "Seller " + (id.split("-").firstOrNull()?.uppercase() ?: id.take(8))
}

@Composable
private fun SellerProfileContent(
    user: AuthenticatedUser,
    listings: List<ListingUiModel>,
    onNavigateToDetail: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            SellerHeader(user = user)
        }

        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Listings by ${user.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MarketplaceDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (listings.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("This seller has no other active listings.", color = Color.Gray)
                }
            }
        } else {
            items(listings) { listing ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    SellerListingItem(
                        listing = listing,
                        onClick = { onNavigateToDetail(listing.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SellerHeader(user: AuthenticatedUser) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                Brush.verticalGradient(
                    listOf(Color.White, MarketplaceBackground)
                )
            )
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MarketplaceWhite)
                    .border(3.dp, MarketplaceWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MarketplaceYellow
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MarketplaceDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Surface(
                color = MarketplaceWhite.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = MarketplaceYellow)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${user.rating}/5 Seller Rating", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
private fun SellerListingItem(listing: ListingUiModel, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
        modifier = Modifier.fillMaxWidth()
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
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = listing.name, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(text = "$${listing.price.toInt()}", color = MarketplaceYellow, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MarketplaceBackground,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = listing.condition.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.LightGray)
        }
    }
}
