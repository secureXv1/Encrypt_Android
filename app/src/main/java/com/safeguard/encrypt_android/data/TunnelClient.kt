package com.safeguard.encrypt_android.data

import android.util.Log
import okhttp3.*
import org.json.JSONObject

class TunnelClient(
    private val tunnelId: Int,
    private val alias: String,
    private val uuid: String,
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    // Callbacks públicos
    var onMessageReceived: ((String) -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    fun connect() {
        val request = Request.Builder()
            .url("ws://symbolsaps.ddns.net:8000/ws/tunnels/$tunnelId/$uuid/$alias")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("TunnelClient", "🟢 Conectado al túnel $tunnelId como $alias")
                onConnected?.invoke()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d("TunnelClient", "📩 Mensaje recibido: $text")
                onMessageReceived?.invoke(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.w("TunnelClient", "🟡 Cerrando conexión ($code): $reason")
                ws.close(code, null)
                onDisconnected?.invoke()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("TunnelClient", "🔴 Error: ${t.localizedMessage}", t)
                onDisconnected?.invoke()
            }
        })
    }

    fun sendMessage(content: String) {
        val json = JSONObject().apply {
            put("type", "text")
            put("content", content)
        }

        val sent = webSocket?.send(json.toString()) ?: false
        if (!sent) {
            Log.w("TunnelClient", "⚠️ No se pudo enviar el mensaje. WebSocket no está conectado.")
        }
    }

    fun close() {
        webSocket?.close(1000, "Cerrado por el cliente")
        webSocket = null
    }
}
