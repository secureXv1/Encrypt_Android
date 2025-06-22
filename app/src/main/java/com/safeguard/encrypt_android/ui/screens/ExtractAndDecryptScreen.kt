// ui/screens/ExtractAndDecryptScreen.kt
package com.safeguard.encrypt_android.ui.screens

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
import com.safeguard.encrypt_android.crypto.CryptoController
import com.safeguard.encrypt_android.utils.indexOfFirstSlice
import java.io.File

@Composable
fun ExtractAndDecryptScreen() {
    val context = LocalContext.current
    var containerUri by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf("") }
    var privateKeyPem by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val pickContainer = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        containerUri = uri
    }

    val pickPrivateKey = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri?.let {
            val pem = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
            privateKeyPem = pem ?: ""
        }
    }

    Column(Modifier.padding(16.dp)) {
        Text("🔍 Extraer y Descifrar Oculto", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(12.dp))
        Button(onClick = { pickContainer.launch(arrayOf("*/*")) }) {
            Text("Seleccionar archivo contenedor")
        }
        Text("Archivo: ${containerUri?.lastPathSegment ?: "ninguno"}")

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña (si aplica)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        Button(onClick = { pickPrivateKey.launch(arrayOf("*/*")) }) {
            Text("Seleccionar clave privada (si aplica)")
        }
        Text("Clave: ${if (privateKeyPem.isNotBlank()) "✔️ cargada" else "❌ no cargada"}")

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (containerUri == null) {
                message = "⚠️ Selecciona un archivo válido."
                return@Button
            }

            try {
                val input = context.contentResolver.openInputStream(containerUri!!)!!
                val content = input.readBytes()
                val delimiter = "<<--BETTY_START-->>".toByteArray()
                val index = content.indexOfFirstSlice(delimiter)

                if (index == -1) {
                    message = "❌ No se encontró archivo oculto."
                    return@Button
                }

                val jsonBytes = content.copyOfRange(index + delimiter.size, content.size)
                val tempJsonFile = File.createTempFile("extraido", ".json", context.cacheDir).apply {
                    writeBytes(jsonBytes)
                }

                val outputFile = File(context.cacheDir, "descifrado_final")

                val decryptedFile = CryptoController.decrypt(
                    inputFile = tempJsonFile,
                    promptForPassword = { password },
                    privateKeyPEM = if (privateKeyPem.isNotBlank()) privateKeyPem else null
                )


                message = "✅ Descifrado exitoso:\n${outputFile.absolutePath}"
                Toast.makeText(context, "Archivo extraído y descifrado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                message = "❌ Error: ${e.message}"
            }
        }) {
            Text("Extraer y Descifrar")
        }

        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}
