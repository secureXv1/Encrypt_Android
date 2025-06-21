// ChatTab.kt
package com.safeguard.encrypt_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun ChatTab(tunnelId: String, alias: String) {
    var message by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                Text(text = msg, color = Color.White, modifier = Modifier.padding(4.dp))
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
            Button(onClick = {
                if (message.isNotBlank()) {
                    messages.add("Tú: $message")
                    // TODO: Enviar por socket
                    message = ""
                }
            }, modifier = Modifier.padding(start = 8.dp)) {
                Text("Enviar")
            }
        }
    }
}
