package com.university.marketplace.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.marketplace.domain.Conversation
import com.university.marketplace.ui.theme.MarketplaceBackground
import com.university.marketplace.ui.theme.MarketplaceDark
import com.university.marketplace.ui.theme.MarketplaceWhite
import com.university.marketplace.ui.theme.MarketplaceYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    isOnline: Boolean,
    onBack: () -> Unit,
    onNavigateToChat: (conversationId: String, otherUserName: String) -> Unit,
    viewModel: ConversationListViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isOnline) {
        if (isOnline) viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) },
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
                is ConversationListUiState.Loading ->
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MarketplaceYellow
                    )
                is ConversationListUiState.Empty ->
                    Text(
                        "No conversations yet.",
                        modifier = Modifier.align(Alignment.Center),
                        color = MarketplaceDark.copy(alpha = 0.6f)
                    )
                is ConversationListUiState.Error ->
                    Text(
                        state.message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = Color.Red
                    )
                is ConversationListUiState.Success ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.conversations, key = { it.id }) { conv ->
                            ConversationCard(
                                conversation = conv,
                                onClick = { onNavigateToChat(conv.id, conv.otherUserName) }
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun ConversationCard(conversation: Conversation, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MarketplaceWhite),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(conversation.otherUserName, fontWeight = FontWeight.Bold)
                Text(
                    conversation.lastMessageAt.take(10),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = conversation.listingTitle,
                fontSize = 12.sp,
                color = MarketplaceYellow,
                modifier = Modifier.padding(top = 2.dp)
            )
            conversation.lastMessageBody?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = MarketplaceDark.copy(alpha = 0.7f),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
