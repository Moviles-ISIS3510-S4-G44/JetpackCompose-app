package com.university.marketplace.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.chat.ChatWebSocketClient
import com.university.marketplace.data.chat.WsEvent
import com.university.marketplace.data.toUserFriendlyMessage
import com.university.marketplace.domain.ChatMessage
import com.university.marketplace.domain.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Success(
        val messages: List<ChatMessage>,
        val wsConnected: Boolean = true
    ) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val wsClient: ChatWebSocketClient,
    private val conversationId: String,
    private val token: String,
    val currentUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        wsClient.connect(conversationId, token)
        viewModelScope.launch {
            loadHistory()
            collectWsEvents()
        }
    }

    private suspend fun loadHistory() {
        try {
            val messages = chatRepository.getMessages(conversationId)
            _uiState.value = ChatUiState.Success(messages)
        } catch (e: Exception) {
            _uiState.value = ChatUiState.Error(
                e.toUserFriendlyMessage(fallback = "Failed to load messages")
            )
        }
    }

    private suspend fun collectWsEvents() {
        wsClient.events.collect { event ->
            when (event) {
                is WsEvent.MessageReceived -> {
                    val incoming = event.message
                    val newMsg = ChatMessage(
                        id = incoming.id,
                        conversationId = incoming.conversationId,
                        senderId = incoming.senderId,
                        body = incoming.body,
                        sentAt = incoming.sentAt
                    )
                    _uiState.update { state ->
                        if (state is ChatUiState.Success) {
                            if (state.messages.any { it.id == newMsg.id }) state
                            else state.copy(messages = state.messages + newMsg)
                        } else {
                            ChatUiState.Success(listOf(newMsg))
                        }
                    }
                }
                is WsEvent.Error, is WsEvent.Closed ->
                    _uiState.update { s ->
                        if (s is ChatUiState.Success) s.copy(wsConnected = false) else s
                    }
            }
        }
    }

    fun sendMessage(body: String) {
        val trimmed = body.trim()
        if (trimmed.isBlank()) return
        wsClient.sendMessage(trimmed)
    }

    override fun onCleared() {
        wsClient.disconnect()
        super.onCleared()
    }
}
