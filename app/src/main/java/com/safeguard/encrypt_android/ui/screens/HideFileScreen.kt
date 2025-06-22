package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun HideFileScreen() {
    val context = LocalContext.current

    var encryptedUri by remember { mutableStateOf<Uri?>(null) }
    var containerUri by remember { mutableStateOf<Uri?>(null) }
    var encryptedName by remember { mutableStateOf("ninguno") }
    var containerName by remember { mutableStateOf("ninguno") }
    var resultMessage by remember { mutableStateOf("") }

    val pickEncryptedFile = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        encryptedUri = uri
        encryptedName = uri?.let { getFileNameWithExtensionFromUri(context, it) } ?: "ninguno"
    }

    val pickContainerFile = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        containerUri = uri
        containerName = uri?.let { getFileNameWithExtensionFromUri(context, it) } ?: "ninguno"
    }

    Column(Modifier.padding(16.dp)) {
        Text("🖼️ Ocultar Archivo Cifrado", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(12.dp))
        Button(onClick = { pickEncryptedFile.launch(arrayOf("*/*")) }) {
            Text("Seleccionar archivo cifrado (.json)")
        }
        Text("Archivo cifrado: $encryptedName")

        Spacer(Modifier.height(12.dp))
        Button(onClick = { pickContainerFile.launch(arrayOf("*/*")) }) {
            Text("Seleccionar archivo contenedor")
        }
        Text("Contenedor: $containerName")

        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            if (encryptedUri == null || containerUri == null) {
                resultMessage = "⚠️ Selecciona ambos archivos."
                return@Button
            }

            try {
                val contBytes = context.contentResolver.openInputStream(containerUri!!)!!.readBytes()
                val cifBytes = context.contentResolver.openInputStream(encryptedUri!!)!!.readBytes()

                val outputDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Encrypt_Android"
                ).apply { mkdirs() }

                val resultFile = File(outputDir, containerName)
                val delimiter = "<<--BETTY_START-->>".toByteArray()

                resultFile.writeBytes(contBytes + delimiter + cifBytes)

                resultMessage = "✅ Archivo oculto guardado en:\n${resultFile.absolutePath}"
                Toast.makeText(context, "Ocultamiento exitoso", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                resultMessage = "❌ Error: ${e.message}"
            }
        }) {
            Text("Ocultar archivo")
        }

        Spacer(Modifier.height(16.dp))
        Text(resultMessage, style = MaterialTheme.typography.bodyMedium)
    }
}

// ✅ Función robusta para obtener nombre + extensión del archivo
fun getFileNameWithExtensionFromUri(context: Context, uri: Uri): String {
    val contentResolver = context.contentResolver
    var name: String? = null
    var extension: String? = null

    val cursor = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex >= 0) {
            name = it.getString(nameIndex)
        }
    }

    if (name != null && !name.contains(".")) {
        val type = contentResolver.getType(uri)
        extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(type ?: "")
        if (extension != null) {
            name += ".$extension"
        }
    }

    return name ?: "archivo_oculto"
}
