package com.university.marketplace.domain

data class Conversation(
    val id: String,
    val listingId: String,
    val buyerId: String,
    val sellerId: String,
    val createdAt: String,
    val lastMessageAt: String,
    val otherUserName: String,
    val otherUserId: String,
    val listingTitle: String,
    val lastMessageBody: String?
)
