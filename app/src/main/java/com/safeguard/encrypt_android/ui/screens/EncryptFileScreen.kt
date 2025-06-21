// ui/screens/EncryptFileScreen.kt
package com.safeguard.encrypt_android.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.safeguard.encrypt_android.crypto.CryptoController
import com.safeguard.encrypt_android.crypto.CryptoController.EncryptionMethod
import java.io.File

@Composable
fun EncryptFileScreen() {
    val context = LocalContext.current
    var inputUri by remember { mutableStateOf<Uri?>(null) }
    var method by remember { mutableStateOf<EncryptionMethod?>(null) }
    var password by remember { mutableStateOf("") }
    var publicKeyPem by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }

    val pickInputFile = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        inputUri = uri
    }

    val pickPemFile = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri?.let {
            publicKeyPem = context.contentResolver.openInputStream(it)
                ?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
        }
    }

    Column(Modifier.padding(16.dp)) {
        Text("📦 Cifrar Archivo", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Button(onClick = { pickInputFile.launch(arrayOf("*/*")) }) {
            Text("Seleccionar archivo a cifrar")
        }
        Text("Archivo: ${inputUri?.lastPathSegment ?: "ninguno"}")

        Spacer(Modifier.height(20.dp))
        Text("Método de cifrado:", style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = method == EncryptionMethod.PASSWORD,
                onClick = { method = EncryptionMethod.PASSWORD }
            )
            Text("Contraseña", Modifier.padding(end = 16.dp))

            RadioButton(
                selected = method == EncryptionMethod.PUBLIC_KEY,
                onClick = { method = EncryptionMethod.PUBLIC_KEY }
            )
            Text("Llave pública")
        }

        if (method == EncryptionMethod.PASSWORD) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (method == EncryptionMethod.PUBLIC_KEY) {
            Button(onClick = { pickPemFile.launch(arrayOf("*/*")) }) {
                Text("Seleccionar archivo .pem")
            }
            Text("Llave: ${if (publicKeyPem.isNotBlank()) "✔️ cargada" else "❌ no cargada"}")
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            try {
                if (inputUri == null || method == null) {
                    resultMessage = "⚠️ Selecciona archivo y método."
                    return@Button
                }

                val inputFile = File.createTempFile("input", null, context.cacheDir).apply {
                    context.contentResolver.openInputStream(inputUri!!)?.use {
                        writeBytes(it.readBytes())
                    }
                }
                val outputFile = File(context.cacheDir, "archivo_cifrado.json")

                CryptoController.encrypt(
                    inputFile = inputFile,
                    method = method!!,
                    outputFile = outputFile,
                    password = if (method == EncryptionMethod.PASSWORD) password else null,
                    publicKeyPEM = if (method == EncryptionMethod.PUBLIC_KEY) publicKeyPem else null
                )

                resultMessage = "✅ Archivo cifrado guardado en:\n${outputFile.absolutePath}"
                Toast.makeText(context, "Cifrado exitoso", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                resultMessage = "❌ Error al cifrar: ${e.message}"
            }
        }) {
            Text("CIFRAR")
        }

        Spacer(Modifier.height(16.dp))
        Text(resultMessage, style = MaterialTheme.typography.bodyMedium)
    }
}
