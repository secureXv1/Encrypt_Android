package com.safeguard.encrypt_android.data

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.Socket

class TunnelClient(
    private val tunnelId: Int,
    private val alias: String,
    private val uuid: String,
    private val hostname: String = "ANDROID",
    private val sistema: String = "Android"
) {
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null

    var onMessageReceived: ((String) -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    private var readJob: Job? = null

    fun connect() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = Socket("symbolsaps.ddns.net", 5050)
                writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))

                // Enviar handshake inicial
                val handshake = JSONObject().apply {
                    put("tunnel_id", tunnelId)
                    put("alias", alias)
                    put("uuid", uuid)
                    put("hostname", hostname)
                    put("sistema", sistema)
                }
                writer?.write(handshake.toString() + "\n")
                writer?.flush()

                val response = reader?.readLine()
                if (response == "OK") {
                    Log.d("TunnelClient", "🟢 Conectado al túnel")
                    onConnected?.invoke()
                } else {
                    Log.e("TunnelClient", "❌ Falló el handshake: $response")
                    disconnect()
                    return@launch
                }

                // Leer mensajes continuamente
                readJob = launch {
                    while (true) {
                        val line = reader?.readLine() ?: break
                        Log.d("TunnelClient", "📩 Recibido: $line")
                        onMessageReceived?.invoke(line)
                    }
                }

            } catch (e: Exception) {
                Log.e("TunnelClient", "🔴 Error de conexión", e)
                onDisconnected?.invoke()
            }
        }
    }

    fun sendMessage(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                writer?.write(text + "\n")
                writer?.flush()
                Log.d("TunnelClient", "✉️ Mensaje enviado: $text")
            } catch (e: Exception) {
                Log.e("TunnelClient", "⚠️ Error enviando mensaje", e)
            }
        }
    }

    fun disconnect() {
        try {
            readJob?.cancel()
            writer?.close()
            reader?.close()
            socket?.close()
            onDisconnected?.invoke()
            Log.d("TunnelClient", "🔌 Desconectado")
        } catch (e: Exception) {
            Log.e("TunnelClient", "⚠️ Error al cerrar conexión", e)
        }
    }
}
