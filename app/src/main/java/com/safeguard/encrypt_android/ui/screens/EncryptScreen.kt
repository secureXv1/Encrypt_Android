// ui/screens/EncryptScreen.kt
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
import com.safeguard.encrypt_android.crypto.CryptoController.EncryptionMethod
import java.io.File

@Composable
fun EncryptScreen() {
    val context = LocalContext.current
    var inputFileUri by remember { mutableStateOf<Uri?>(null) }
    var outputFileName by remember { mutableStateOf("archivo_cifrado.json") }
    var method by remember { mutableStateOf<EncryptionMethod?>(null) }
    var password by remember { mutableStateOf("") }
    var publicKeyPem by remember { mutableStateOf("") }

    val pickFileLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        inputFileUri = uri
    }

    val pickKeyLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri?.let {
            val pem = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            publicKeyPem = pem ?: ""
        }
    }

    Column(Modifier.padding(16.dp)) {
        Text("🔐 Cifrado de Archivos", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(8.dp))

        Button(onClick = { pickFileLauncher.launch(arrayOf("*/*")) }) {
            Text("Seleccionar archivo a cifrar")
        }

        Text("Archivo: ${inputFileUri?.lastPathSegment ?: "ninguno"}", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(16.dp))

        Text("Método de cifrado:")
        Row {
            RadioButton(selected = method == EncryptionMethod.PASSWORD, onClick = { method = EncryptionMethod.PASSWORD })
            Text("Contraseña", Modifier.padding(end = 16.dp))
            RadioButton(selected = method == EncryptionMethod.PUBLIC_KEY, onClick = { method = EncryptionMethod.PUBLIC_KEY })
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
            Button(onClick = { pickKeyLauncher.launch(arrayOf("*/*")) }) {
                Text("Seleccionar archivo PEM")
            }
            Text("Llave: ${if (publicKeyPem.isNotBlank()) "✔️ cargada" else "❌ no cargada"}", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            try {
                val inputStream = inputFileUri?.let { context.contentResolver.openInputStream(it) }
                val tempInputFile = File.createTempFile("input", null, context.cacheDir).apply {
                    inputStream?.use { input -> writeBytes(input.readBytes()) }
                }
                val outputFile = File(context.cacheDir, outputFileName)

                CryptoController.encrypt(
                    inputFile = tempInputFile,
                    method = method!!,
                    outputFile = outputFile,
                    password = if (method == EncryptionMethod.PASSWORD) password else null,
                    publicKeyPEM = if (method == EncryptionMethod.PUBLIC_KEY) publicKeyPem else null
                )

                Toast.makeText(context, "✅ Archivo cifrado:\n${outputFile.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }) {
            Text("CIFRAR")
        }
    }
}
