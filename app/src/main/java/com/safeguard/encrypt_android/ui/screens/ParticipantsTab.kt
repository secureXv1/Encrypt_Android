package com.safeguard.encrypt_android.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun ParticipantsTab(tunnelId: String) {
    var participantes by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(tunnelId) {
        participantes = obtenerParticipantes(tunnelId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1B1E))
            .padding(16.dp)
    ) {
        Text("👥 Participantes", color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(participantes) { alias ->
                Text(
                    text = alias,
                    color = Color(0xFF00BCD4),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

suspend fun obtenerParticipantes(tunnelId: String): List<String> = withContext(Dispatchers.IO) {
    try {
        val url = URL("http://symbolsaps.ddns.net:8000/api/tunnels/$tunnelId/participants") // Ajusta IP si estás en dispositivo real
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"

        if (conn.responseCode == 200) {
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONArray(org.json.JSONObject(response).getJSONArray("participants").toString())

            return@withContext (0 until json.length()).flatMap { i ->
                val obj = json.getJSONObject(i)
                val aliases = obj.getJSONArray("aliases")
                (0 until aliases.length()).map { j -> aliases.getString(j) }
            }
        } else {
            Log.e("ParticipantsTab", "Error: ${conn.responseCode}")
        }
    } catch (e: Exception) {
        Log.e("ParticipantsTab", "Excepción: ${e.message}")
    }
    return@withContext emptyList()
}
