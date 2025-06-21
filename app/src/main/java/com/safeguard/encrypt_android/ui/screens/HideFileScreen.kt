// ui/screens/HideFileScreen.kt
package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.net.Uri
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
    var resultMessage by remember { mutableStateOf("") }

    val pickEncryptedFile = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        encryptedUri = uri
    }

    val pickContainerFile = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        containerUri = uri
    }

    Column(Modifier.padding(16.dp)) {
        Text("🖼️ Ocultar Archivo Cifrado", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(12.dp))
        Button(onClick = { pickEncryptedFile.launch(arrayOf("*/*")) }) {
            Text("Seleccionar archivo cifrado (.json)")
        }
        Text("Archivo cifrado: ${encryptedUri?.lastPathSegment ?: "ninguno"}")

        Spacer(Modifier.height(12.dp))
        Button(onClick = { pickContainerFile.launch(arrayOf("*/*")) }) {
            Text("Seleccionar archivo contenedor")
        }
        Text("Contenedor: ${containerUri?.lastPathSegment ?: "ninguno"}")

        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            if (encryptedUri == null || containerUri == null) {
                resultMessage = "⚠️ Selecciona ambos archivos."
                return@Button
            }

            try {
                val contInput = context.contentResolver.openInputStream(containerUri!!)!!
                val contBytes = contInput.readBytes()
                val cifInput = context.contentResolver.openInputStream(encryptedUri!!)!!
                val cifBytes = cifInput.readBytes()

                val resultFile = File(context.cacheDir, "oculto.bin")
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
