package com.university.marketplace.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApi {
    @POST("chat/conversations")
    suspend fun getOrCreateConversation(@Body payload: ConversationCreateDto): ConversationDto

    @GET("chat/conversations")
    suspend fun getConversations(): List<ConversationDto>

    @GET("chat/conversations/{id}")
    suspend fun getConversation(@Path("id") id: String): ConversationDto

    @GET("chat/conversations/{id}/messages")
    suspend fun getMessages(@Path("id") conversationId: String): List<MessageDto>
}
