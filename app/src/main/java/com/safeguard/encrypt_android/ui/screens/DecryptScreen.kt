package com.safeguard.encrypt_android.ui.screens

import android.content.Intent
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

import com.safeguard.encrypt_android.ui.components.PemFilePicker
import java.io.File
import androidx.core.content.FileProvider
import com.safeguard.encrypt_android.crypto.CryptoController
import com.safeguard.encrypt_android.utils.openOutputFolder


@Composable
fun DecryptScreen() {
    val context = LocalContext.current

    var inputUri by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf("") }
    var privateKeyPem by remember { mutableStateOf("") }
    var showPemPicker by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }

    val pickJsonLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        inputUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("🛠️ Descifrar Archivo", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Button(onClick = { pickJsonLauncher.launch(arrayOf("application/json")) }) {
            Text("📄 Seleccionar archivo cifrado (.json)")
        }

        Text(
            "Archivo: ${inputUri?.lastPathSegment ?: "ninguno"}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña (si aplica)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Button(onClick = { showPemPicker = !showPemPicker }) {
            Text("🔑 Seleccionar clave privada (opcional)")
        }

        if (privateKeyPem.isNotBlank()) {
            Text("✔️ Clave cargada correctamente", modifier = Modifier.padding(top = 6.dp))
        }

        if (showPemPicker) {
            PemFilePicker(context) { fileName, content ->
                privateKeyPem = content
                showPemPicker = false
                Toast.makeText(context, "Clave $fileName cargada", Toast.LENGTH_SHORT).show()
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                try {
                    if (inputUri == null) {
                        resultMessage = "⚠️ Debes seleccionar un archivo cifrado."
                        return@Button
                    }

                    val inputStream = context.contentResolver.openInputStream(inputUri!!)
                    val tempInputFile = File.createTempFile("cifrado", ".json", context.cacheDir).apply {
                        inputStream?.use { writeBytes(it.readBytes()) }
                    }

                    val outputDir = File(context.getExternalFilesDir(null), "Documents/EncryptApp").apply {
                        if (!exists()) mkdirs()
                    }

                    val originalName = inputUri?.lastPathSegment
                        ?.substringAfterLast("/")
                        ?.substringBeforeLast(".") ?: "archivo"

                    val outputFile = File(outputDir, "${originalName}_Dec")

                    CryptoController.decrypt(
                        inputFile = tempInputFile,
                        outputFile = outputFile,
                        promptForPassword = { password },
                        privateKeyPEM = if (privateKeyPem.isNotBlank()) privateKeyPem else null
                    )

                    resultMessage = "✅ Archivo descifrado:\n${outputFile.absolutePath}"
                    Toast.makeText(context, "Descifrado exitoso", Toast.LENGTH_SHORT).show()

                    // 🟢 Abrir carpeta automáticamente
                    openOutputFolder(context, outputFile.parentFile!!)

                } catch (e: Exception) {
                    resultMessage = "❌ Error: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔓 DESCIFRAR")
        }

        if (resultMessage.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(resultMessage, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

