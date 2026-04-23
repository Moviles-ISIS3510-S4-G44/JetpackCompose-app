package com.university.marketplace.ui.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.marketplace.domain.Purchase
import com.university.marketplace.ui.theme.MarketplaceBackground
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceWhite
import com.university.marketplace.ui.theme.MarketplaceYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseHistoryScreen(
    isOnline: Boolean,
    onBack: () -> Unit,
    viewModel: PurchaseHistoryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isOnline) {
        if (isOnline) viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Purchases", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MarketplaceWhite)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MarketplaceBackground)
                .padding(padding)
        ) {
            when (val state = uiState) {
                is PurchaseHistoryUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MarketplaceDark
                    )
                }
                is PurchaseHistoryUiState.Empty -> {
                    Text(
                        text = "No purchases yet.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MarketplaceDark.copy(alpha = 0.6f)
                    )
                }
                is PurchaseHistoryUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = Color.Red
                    )
                }
                is PurchaseHistoryUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.purchases, key = { it.id }) { purchase ->
                            PurchaseCard(
                                purchase = purchase,
                                isOnline = isOnline,
                                onRate = { rating -> viewModel.rateSeller(purchase.id, rating) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseCard(
    purchase: Purchase,
    isOnline: Boolean,
    onRate: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Order #${purchase.id.take(8)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$${purchase.priceAtPurchase}",
                    fontWeight = FontWeight.ExtraBold,
                    color = MarketplaceYellow
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = purchase.purchasedAt.take(10),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (purchase.sellerRating != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your rating: ", fontSize = 13.sp, color = MarketplaceDark.copy(alpha = 0.7f))
                    repeat(5) { i ->
                        Icon(
                            imageVector = if (i < purchase.sellerRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = MarketplaceYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Text(
                    "Rate the seller:",
                    fontSize = 13.sp,
                    color = MarketplaceDark.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    repeat(5) { i ->
                        IconButton(
                            onClick = { if (isOnline) onRate(i + 1) },
                            modifier = Modifier.size(32.dp),
                            enabled = isOnline
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.StarBorder,
                                contentDescription = "${i + 1} stars",
                                tint = MarketplaceYellow
                            )
                        }
                    }
                }
            }
        }
    }
}
