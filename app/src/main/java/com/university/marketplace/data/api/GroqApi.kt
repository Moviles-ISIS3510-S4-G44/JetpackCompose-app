package com.university.marketplace.data.api

import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApi {
    @POST("v1/chat/completions")
    suspend fun completeChat(
        @Header("Authorization") apiKey: String,
        @Body request: GroqRequest
    ): GroqResponse
}

data class GroqRequest(
    val model: String = "gemma2-9b-it",
    val messages: List<GroqMessage>,
    @Json(name = "response_format") val responseFormat: GroqResponseFormat = GroqResponseFormat()
)

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqResponseFormat(
    val type: String = "json_object"
)

data class GroqResponse(
    val choices: List<GroqChoice>
)

data class GroqChoice(
    val message: GroqMessage
)

data class SearchIntent(
    val product_name: String?,
    val max_price: Double?,
    val category: String?,
    val condition: String?,
    val proximity_preference: Double?,
    val sort_order: String?
)
