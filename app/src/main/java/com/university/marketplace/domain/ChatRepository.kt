package com.university.marketplace.domain

interface ChatRepository {
    suspend fun getOrCreateConversation(listingId: String): Conversation
    suspend fun getConversations(): List<Conversation>
    suspend fun getMessages(conversationId: String): List<ChatMessage>
}
