package com.safeguard.encrypt_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.safeguard.encrypt_android.data.TunnelClient
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun MessageBubble(message: TunnelMessage, isMine: Boolean) {
    val bubbleColor = if (isMine) Color(0xFF00BCD4) else Color.DarkGray
    val textColor = if (isMine) Color.Black else Color.White
    val alignment = if (isMine) Arrangement.End else Arrangement.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (!isMine) {
            Text(
                text = message.alias,
                color = Color.LightGray,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }

        Surface(
            color = bubbleColor,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Row(modifier = Modifier.padding(10.dp)) {
                when (message.type) {
                    "text" -> Text(message.content, color = textColor)
                    "archivo" -> Text("📎 ${message.content}", color = textColor)
                    else -> Text("🔸 ${message.content}", color = textColor)
                }
            }
        }

        Text(
            text = formatTime(message.timestamp),
            color = Color.Gray,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
        )
    }
}

@Composable
fun ChatTab(
    tunnelId: String,
    alias: String,
    tunnelClient: TunnelClient?,
    messages: SnapshotStateList<TunnelMessage>
) {
    var message by remember { mutableStateOf("") }

    // Recibir mensajes del servidor
    LaunchedEffect(tunnelClient) {
        tunnelClient?.onMessageReceived = { incoming ->
            val parsed = parseTunnelMessage(incoming)
            parsed?.let { messages.add(it) }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                MessageBubble(message = msg, isMine = msg.alias == alias)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje...") },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.DarkGray,
                    focusedContainerColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Button(
                onClick = {
                    if (message.isNotBlank()) {
                        val json = JSONObject().apply {
                            put("type", "text")
                            put("contenido", message)
                        }
                        tunnelClient?.sendMessage(json.toString())

                        messages.add(
                            TunnelMessage(
                                tunnelId = tunnelId.toInt(),
                                alias = alias,
                                type = "text",
                                content = message,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        message = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Enviar")
            }
        }
    }
}
