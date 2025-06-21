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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun FilesTab(tunnelId: String) {
    var archivos by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    LaunchedEffect(tunnelId) {
        archivos = obtenerArchivos(tunnelId)
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0E1B1E))
        .padding(16.dp)
    ) {
        Text("📁 Archivos Compartidos", color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(archivos) { (filename, url) ->
                Text(
                    text = filename,
                    color = Color(0xFF00BCD4),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

suspend fun obtenerArchivos(tunnelId: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
    try {
        val url = URL("http://symbolsaps.ddns.net:8000/api/tunnels/$tunnelId/files")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"

        if (conn.responseCode == 200) {
            val response = conn.inputStream.bufferedReader().readText()
            val filesArray = JSONObject(response).getJSONArray("files")

            val result = mutableListOf<Pair<String, String>>()
            for (i in 0 until filesArray.length()) {
                val file = filesArray.getJSONObject(i)
                result.add(file.getString("filename") to file.getString("url"))
            }

            Log.d("FilesTab", "Archivos recibidos: $result")
            return@withContext result
        } else {
            Log.e("FilesTab", "Error: ${conn.responseCode}")
        }
    } catch (e: Exception) {
        Log.e("FilesTab", "Excepción: ${e.message}")
    }
    return@withContext emptyList()
}

