package com.university.marketplace.domain

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String,
    val sentAt: String
)
