package com.safeguard.encrypt_android.ui.screens

import android.net.Uri
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeguard.encrypt_android.data.TunnelClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import android.os.Handler
import android.os.Looper

import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import com.safeguard.encrypt_android.utils.UuidUtils.getClientUUID


fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun MessageBubble(message: TunnelMessage, isOwnMessage: Boolean) {
    val context = LocalContext.current
    val bubbleColor = if (isOwnMessage) Color(0xFF00BCD4) else Color(0xFF333333)
    val textColor = if (isOwnMessage) Color.Black else Color.White

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                        .clickable { descargarArchivo(context, message.content, nombre) }
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
            text = formatTime(message.timestamp),
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
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val nombre = obtenerNombreArchivoDesdeUri(context, it)
            val inputStream = context.contentResolver.openInputStream(it)

            if (inputStream != null) {
                uploadFileToServer(
                    context = context,
                    inputStream = inputStream,
                    fileName = nombre,
                    alias = alias,
                    tunnelId = tunnelId,
                    uuid = getClientUUID(context),
                    onSuccess = { fileUrl ->
                        val json = JSONObject().apply {
                            put("type", "file")
                            put("contenido", fileUrl)
                            put("filename", nombre)
                        }
                        tunnelClient?.sendMessage(json.toString())

                        messages.add(
                            TunnelMessage(
                                tunnelId = tunnelId.toInt(),
                                alias = alias,
                                type = "file",
                                content = fileUrl,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                )

            }
        }
    }



    // Recibir mensajes
    LaunchedEffect(tunnelClient) {
        tunnelClient?.onMessageReceived = { incoming ->
            val parsed = parseTunnelMessage(incoming)
            parsed?.let { messages.add(it) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                MessageBubble(message = msg, isOwnMessage = msg.alias == alias)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📎",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { filePickerLauncher.launch("*/*") }
            )

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

            Text(
                text = "📨",
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable {
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
                    }
            )
        }
    }
}


fun obtenerNombreArchivoDesdeUri(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        it.getString(nameIndex)
    } ?: uri.lastPathSegment ?: "archivo"
}

fun obtenerNombreRealDesdeUrl(url: String): String {
    val filename = url.substringAfterLast("/")
    val partes = filename.split("_")
    return if (partes.size >= 3) partes.subList(2, partes.size).joinToString("_") else filename
}

fun uploadFileToServer(
    context: Context,
    inputStream: InputStream,
    fileName: String,
    alias: String,
    tunnelId: String,
    uuid: String,
    onSuccess: (fileUrl: String) -> Unit
) {
    val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart(
            "file", fileName,
            inputStream.readBytes().toRequestBody(null)
        )
        .addFormDataPart("alias", alias)
        .addFormDataPart("tunnel_id", tunnelId)
        .addFormDataPart("uuid", uuid)
        .build()

    val request = Request.Builder()
        .url("http://symbolsaps.ddns.net:8000/api/upload-file")
        .post(requestBody)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("Upload", "❌ Falló la subida: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                Log.e("Upload", "⚠️ Error del servidor: ${response.code}")
                return
            }

            response.body?.string()?.let {
                try {
                    val json = JSONObject(it)
                    val url = json.getString("url")
                    Handler(Looper.getMainLooper()).post {
                        onSuccess(url)
                    }
                } catch (e: Exception) {
                    Log.e("Upload", "❌ Error al parsear respuesta: ${e.message}")
                }
            }
        }
    })
}

