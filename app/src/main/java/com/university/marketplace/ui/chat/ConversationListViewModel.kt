package com.university.marketplace.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.marketplace.data.toUserFriendlyMessage
import com.university.marketplace.domain.ChatRepository
import com.university.marketplace.domain.Conversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ConversationListUiState {
    data object Loading : ConversationListUiState
    data object Empty : ConversationListUiState
    data class Success(val conversations: List<Conversation>) : ConversationListUiState
    data class Error(val message: String) : ConversationListUiState
}

class ConversationListViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConversationListUiState>(ConversationListUiState.Loading)
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = ConversationListUiState.Loading
            try {
                val conversations = chatRepository.getConversations()
                _uiState.value = if (conversations.isEmpty()) ConversationListUiState.Empty
                else ConversationListUiState.Success(conversations)
            } catch (e: Exception) {
                _uiState.value = ConversationListUiState.Error(
                    e.toUserFriendlyMessage(fallback = "Failed to load conversations")
                )
            }
        }
    }
}
