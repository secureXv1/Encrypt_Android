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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext


@Composable
fun FilesTab(tunnelId: String, messages: List<TunnelMessage>) {
    val context = LocalContext.current

    val archivos = remember(messages) {
        val extensionesArchivo = listOf(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".svg",
            ".mp3", ".wav", ".ogg", ".mp4", ".avi", ".mkv", ".mov",
            ".zip", ".rar", ".7z", ".tar", ".gz", ".json", ".txt", ".csv"
        )

        messages.filter { msg ->
            msg.content.startsWith("http") &&
                    extensionesArchivo.any { ext -> msg.content.lowercase().endsWith(ext) }
        }.map {
            val nombre = obtenerNombreRealDesdeUrl(it.content)
            nombre to it.content
        }.distinctBy { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1B1E))
            .padding(16.dp)
    ) {
        Text("📁 Archivos Compartidos", color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        if (archivos.isEmpty()) {
            Text("No se han compartido archivos aún", color = Color.Gray)
        } else {
            LazyColumn {
                items(archivos) { (nombre, url) ->
                    Row(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .fillMaxWidth()
                            .clickable { descargarArchivo(context, url, nombre) }
                    ) {
                        Text(text = "📄", fontSize = 20.sp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = nombre, color = Color(0xFF00BCD4))
                    }
                }
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


fun descargarArchivo(context: Context, url: String, nombre: String) {
    try {
        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri)
        request.setTitle("Descargando $nombre")
        request.setDescription("Archivo desde túnel")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nombre)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(context, "📥 Descargando $nombre...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
