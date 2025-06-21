package com.safeguard.encrypt_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.safeguard.encrypt_android.data.TunnelClient
import com.safeguard.encrypt_android.utils.UuidUtils.getClientUUID

data class TunnelMessage(
    val tunnelId: Int,
    val alias: String,
    val type: String,
    val content: String,
    val timestamp: Long
)

fun parseTunnelMessage(raw: String): TunnelMessage? {
    return try {
        val outer = org.json.JSONObject(raw)
        val inner = org.json.JSONObject(outer.getString("contenido"))

        TunnelMessage(
            tunnelId = outer.getInt("tunnel_id"),
            alias = outer.getString("alias"),
            type = inner.getString("tipo"),
            content = inner.getString("contenido"),
            timestamp = inner.optLong("enviado_en", System.currentTimeMillis())
        )
    } catch (e: Exception) {
        println("❌ Error al parsear mensaje: ${e.message}")
        null
    }
}

@Composable
fun TunnelSessionScreen(
    navController: NavController,
    tunnelId: String,
    alias: String
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Chat", "Participantes", "Archivos")
    val context = LocalContext.current
    val uuid = remember { getClientUUID(context) }

    val tunnelClient = remember { mutableStateOf<TunnelClient?>(null) }
    val messages = remember { mutableStateListOf<TunnelMessage>() }

    LaunchedEffect(Unit) {
        val client = TunnelClient(
            tunnelId = tunnelId.toInt(),
            alias = alias,
            uuid = uuid
        )

        client.onMessageReceived = { raw ->
            parseTunnelMessage(raw)?.let { messages.add(it) }
        }

        client.onConnected = { println("🟢 Conectado al túnel") }
        client.onDisconnected = { println("🔴 Desconectado del túnel") }

        client.connect()
        tunnelClient.value = client
    }

    DisposableEffect(Unit) {
        onDispose {
            tunnelClient.value?.disconnect()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1B1E))
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color(0xFF1C2A2D),
            contentColor = Color(0xFF00BCD4),
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTabIndex == index) Color(0xFF00BCD4) else Color.White
                        )
                    }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> ChatTab(tunnelId = tunnelId, alias = alias, tunnelClient = tunnelClient.value, messages = messages)
            1 -> ParticipantsTab(tunnelId = tunnelId)
            2 -> FilesTab(tunnelId = tunnelId)
        }
    }
}
