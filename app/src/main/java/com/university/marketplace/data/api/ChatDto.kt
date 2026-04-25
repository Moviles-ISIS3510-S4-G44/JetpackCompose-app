package com.university.marketplace.data.api

import com.squareup.moshi.Json

data class ConversationCreateDto(
    @Json(name = "listing_id") val listingId: String
)

data class ParticipantDto(
    val id: String,
    val name: String
)

data class ConversationDto(
    val id: String,
    @Json(name = "listing_id") val listingId: String,
    @Json(name = "buyer_id") val buyerId: String,
    @Json(name = "seller_id") val sellerId: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "last_message_at") val lastMessageAt: String,
    @Json(name = "other_user") val otherUser: ParticipantDto,
    @Json(name = "listing_title") val listingTitle: String,
    @Json(name = "last_message_body") val lastMessageBody: String?
)

data class MessageDto(
    val id: String,
    @Json(name = "conversation_id") val conversationId: String,
    @Json(name = "sender_id") val senderId: String,
    val body: String,
    @Json(name = "sent_at") val sentAt: String
)
