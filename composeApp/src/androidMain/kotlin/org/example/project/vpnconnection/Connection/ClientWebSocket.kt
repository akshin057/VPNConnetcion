package org.example.project.vpnconnection.Connection

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

import okhttp3.*
import java.nio.ByteBuffer

class ClientWebSocket(
    private val domain: String,
    private val uuid: String,
    private val path: String
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val url = "wss://$domain/$path"
    private var webSocket: WebSocket? = null

    fun connect() {
        val request = Request.Builder().url(url).build()
        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "✅ Connected to $url")
                this@ClientWebSocket.webSocket = webSocket
                webSocket.send(buildVlessRequest())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "📥 Received: $text")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d("WebSocket", "📥 Binary data: ${bytes.hex()}")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "❌ WebSocket error: ${t.message}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "🔌 Closing connection: $code $reason")
                webSocket.close(1000, null)
            }
        }

        webSocket = client.newWebSocket(request, webSocketListener)
    }

    fun sendData(data: String) {
        webSocket?.send(data) ?: Log.e("WebSocket", "❌ WebSocket is not connected!")
    }

    fun disconnect() {
        webSocket?.close(1000, "Disconnecting WebSocket")
    }

    private fun buildVlessRequest(): ByteString {
        val requestBytes = ByteArray(24) // Здесь нужно сгенерировать VLESS-запрос
        return ByteString.of(*requestBytes)
    }
}

