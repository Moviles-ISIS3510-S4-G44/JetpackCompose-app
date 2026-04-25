package com.university.marketplace.data.chat

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

sealed interface WsEvent {
    data class MessageReceived(val message: IncomingWsMessage) : WsEvent
    data class Error(val cause: Throwable?) : WsEvent
    data object Closed : WsEvent
}

data class IncomingWsMessage(
    val event: String,
    val id: String,
    @Json(name = "conversation_id") val conversationId: String,
    @Json(name = "sender_id") val senderId: String,
    val body: String,
    @Json(name = "sent_at") val sentAt: String
)

data class OutgoingWsMessage(val body: String)

class ChatWebSocketClient(
    private val okHttpClient: OkHttpClient,
    private val wsBaseUrl: String
) {
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val incomingAdapter = moshi.adapter(IncomingWsMessage::class.java)
    private val outgoingAdapter = moshi.adapter(OutgoingWsMessage::class.java)

    private val _events = Channel<WsEvent>(Channel.UNLIMITED)
    val events: Flow<WsEvent> = _events.receiveAsFlow()

    private var webSocket: WebSocket? = null

    fun connect(conversationId: String, token: String) {
        val url = "${wsBaseUrl}chat/conversations/$conversationId/ws?token=$token"
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = runCatching { incomingAdapter.fromJson(text) }.getOrNull()
                if (msg != null) _events.trySend(WsEvent.MessageReceived(msg))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _events.trySend(WsEvent.Error(t))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _events.trySend(WsEvent.Closed)
            }
        })
    }

    fun sendMessage(body: String) {
        val json = outgoingAdapter.toJson(OutgoingWsMessage(body))
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "User left")
        webSocket = null
    }
}
