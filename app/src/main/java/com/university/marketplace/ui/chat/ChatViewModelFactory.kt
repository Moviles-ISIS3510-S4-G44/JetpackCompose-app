package com.university.marketplace.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.university.marketplace.data.chat.ChatWebSocketClient
import com.university.marketplace.domain.ChatRepository

class ChatViewModelFactory(
    private val chatRepository: ChatRepository,
    private val wsClient: ChatWebSocketClient,
    private val conversationId: String,
    private val token: String,
    private val currentUserId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(
                chatRepository = chatRepository,
                wsClient = wsClient,
                conversationId = conversationId,
                token = token,
                currentUserId = currentUserId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
