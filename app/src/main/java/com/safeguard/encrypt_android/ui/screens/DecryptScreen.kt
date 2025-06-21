// ui/screens/DecryptScreen.kt
package com.safeguard.encrypt_android.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.safeguard.encrypt_android.crypto.CryptoController
import java.io.File

@Composable
fun DecryptScreen() {
    val context = LocalContext.current

    var inputUri by remember { mutableStateOf<Uri?>(null) }
    var outputFileName by remember { mutableStateOf("archivo_descifrado") }
    var password by remember { mutableStateOf("") }
    var privateKeyPem by remember { mutableStateOf("") }

    val pickJsonLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        inputUri = uri
    }

    val pickKeyLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri?.let {
            val key = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            privateKeyPem = key ?: ""
        }
    }

    Column(Modifier.padding(16.dp)) {
        Text("🔓 Descifrado de Archivos", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        Button(onClick = { pickJsonLauncher.launch(arrayOf("application/json")) }) {
            Text("Seleccionar archivo cifrado (.json)")
        }

        Text("Archivo: ${inputUri?.lastPathSegment ?: "ninguno"}", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña (si aplica)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = { pickKeyLauncher.launch(arrayOf("*/*")) }) {
            Text("Seleccionar clave privada PEM (si aplica)")
        }
        Text("Clave: ${if (privateKeyPem.isNotBlank()) "✔️ cargada" else "❌ no cargada"}", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            try {
                val inputStream = inputUri?.let { context.contentResolver.openInputStream(it) }
                val tempInputFile = File.createTempFile("cifrado", ".json", context.cacheDir).apply {
                    inputStream?.use { writeBytes(it.readBytes()) }
                }
                val outputFile = File(context.cacheDir, outputFileName)

                CryptoController.decrypt(
                    inputFile = tempInputFile,
                    outputFile = outputFile,
                    promptForPassword = { password },
                    privateKeyPEM = if (privateKeyPem.isNotBlank()) privateKeyPem else null
                )

                Toast.makeText(context, "✅ Archivo descifrado:\n${outputFile.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }) {
            Text("DESCIFRAR")
        }
    }
}
