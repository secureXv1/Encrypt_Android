package com.safeguard.encrypt_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeguard.encrypt_android.data.TunnelClient
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun MessageBubble(message: TunnelMessage, isOwnMessage: Boolean) {
    val bubbleColor = if (isOwnMessage) Color(0xFF00BCD4) else Color(0xFF333333)
    val textColor = if (isOwnMessage) Color.Black else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        if (!isOwnMessage) {
            Text(
                text = message.alias,
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = bubbleColor,
            tonalElevation = 2.dp
        ) {
            if (message.type == "archivo" || message.content.startsWith("http") && message.content.contains("/uploads/")) {

                val nombre = obtenerNombreRealDesdeUrl(message.content)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(10.dp)
                        .widthIn(max = 260.dp)
                ) {
                    Text(text = "📄", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = nombre, color = textColor)
                }
            } else {
                Text(
                    text = message.content,
                    color = textColor,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Text(
            text = SimpleDateFormat("HH:mm").format(Date(message.timestamp)),
            color = Color.Gray,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp)
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
                MessageBubble(message = msg, isOwnMessage = msg.alias == alias)

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

fun obtenerNombreRealDesdeUrl(url: String): String {
    val filename = url.substringAfterLast("/")
    val partes = filename.split("_")
    return if (partes.size >= 3) {
        partes.subList(2, partes.size).joinToString("_")
    } else {
        filename
    }
}
