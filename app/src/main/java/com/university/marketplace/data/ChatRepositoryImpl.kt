package com.university.marketplace.data

import com.university.marketplace.data.api.ChatApi
import com.university.marketplace.data.api.ConversationCreateDto
import com.university.marketplace.domain.ChatMessage
import com.university.marketplace.domain.ChatRepository
import com.university.marketplace.domain.Conversation

class ChatRepositoryImpl(
    private val api: ChatApi
) : ChatRepository {

    override suspend fun getOrCreateConversation(listingId: String): Conversation =
        api.getOrCreateConversation(ConversationCreateDto(listingId)).toDomain()

    override suspend fun getConversations(): List<Conversation> =
        api.getConversations().map { it.toDomain() }

    override suspend fun getMessages(conversationId: String): List<ChatMessage> =
        api.getMessages(conversationId).map { it.toDomain() }

    private fun com.university.marketplace.data.api.ConversationDto.toDomain() = Conversation(
        id = id,
        listingId = listingId,
        buyerId = buyerId,
        sellerId = sellerId,
        createdAt = createdAt,
        lastMessageAt = lastMessageAt,
        otherUserName = otherUser.name,
        otherUserId = otherUser.id,
        listingTitle = listingTitle,
        lastMessageBody = lastMessageBody
    )

    private fun com.university.marketplace.data.api.MessageDto.toDomain() = ChatMessage(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        body = body,
        sentAt = sentAt
    )
}
