package com.university.marketplace.ui.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.university.marketplace.domain.Purchase
import com.university.marketplace.ui.theme.MarketplaceBackground
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceWhite
import com.university.marketplace.ui.theme.MarketplaceYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    isOnline: Boolean,
    onBack: () -> Unit,
    viewModel: SalesHistoryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isOnline) {
        if (isOnline) viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Sales", fontWeight = FontWeight.Bold) },
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
                is SalesHistoryUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MarketplaceDark
                    )
                }
                is SalesHistoryUiState.Empty -> {
                    Text(
                        text = "No sales yet.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MarketplaceDark.copy(alpha = 0.6f)
                    )
                }
                is SalesHistoryUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = Color.Red
                    )
                }
                is SalesHistoryUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.sales, key = { it.id }) { sale ->
                            SaleCard(sale)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleCard(sale: Purchase) {
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
                    text = "Sale #${sale.id.take(8)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$${sale.priceAtPurchase}",
                    fontWeight = FontWeight.ExtraBold,
                    color = MarketplaceYellow
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sale.purchasedAt.take(10),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            if (sale.sellerRating != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Buyer rated you: ${sale.sellerRating}/5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MarketplaceDark.copy(alpha = 0.7f)
                )
            }
        }
    }
}
