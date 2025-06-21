package com.safeguard.encrypt_android.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
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
import com.safeguard.encrypt_android.ui.components.PemFilePicker
import java.io.File
import androidx.core.content.FileProvider
import com.safeguard.encrypt_android.utils.getFileNameFromUri
import com.safeguard.encrypt_android.utils.openOutputFolder


@Composable
fun EncryptFileScreen() {
    val context = LocalContext.current
    var inputUri by remember { mutableStateOf<Uri?>(null) }
    var method by remember { mutableStateOf<EncryptionMethod?>(null) }
    var password by remember { mutableStateOf("") }
    var publicKeyPem by remember { mutableStateOf("") }
    var showPemPicker by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    var lastEncryptedFile by remember { mutableStateOf<File?>(null) }


    val pickInputFile = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        inputUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("🔐 Cifrar Archivo", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))

        Button(onClick = { pickInputFile.launch(arrayOf("*/*")) }) {
            Text("📁 Seleccionar archivo a cifrar")
        }

        val fileName = inputUri?.let { getFileNameFromUri(context, it) }

        Text(
            "Archivo seleccionado:\n${fileName ?: "Ninguno"}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )


        Spacer(Modifier.height(24.dp))
        Text("Método de cifrado:", style = MaterialTheme.typography.titleMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = method == EncryptionMethod.PASSWORD,
                onClick = {
                    method = EncryptionMethod.PASSWORD
                    showPemPicker = false
                }
            )
            Text("Contraseña", Modifier.padding(end = 16.dp))

            RadioButton(
                selected = method == EncryptionMethod.PUBLIC_KEY,
                onClick = {
                    method = EncryptionMethod.PUBLIC_KEY
                    password = ""
                }
            )
            Text("Llave pública")
        }

        if (method == EncryptionMethod.PASSWORD) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña segura") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }

        if (method == EncryptionMethod.PUBLIC_KEY) {
            Button(onClick = { showPemPicker = !showPemPicker }) {
                Text("\uD83D\uDD11\u200B Seleccionar llave pública")
            }
            if (publicKeyPem.isNotBlank()) {
                Text("✔️ Llave cargada correctamente", modifier = Modifier.padding(top = 6.dp))
            }

            if (showPemPicker) {
                PemFilePicker(context) { fileName, content ->
                    publicKeyPem = content
                    showPemPicker = false
                    Toast.makeText(context, "Llave $fileName cargada", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Reemplaza desde aquí tu bloque final dentro del Column
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
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

                    val outputDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "EncryptApp"
                    ).apply {
                        if (!exists()) mkdirs()
                    }

                    val fileName = getFileNameFromUri(context, inputUri!!)
                    val originalName = fileName?.substringBeforeLast(".") ?: "archivo"

                    val outputFile = File(outputDir, "${originalName}_Cif.json")

                    CryptoController.encrypt(
                        inputFile = inputFile,
                        method = method!!,
                        outputFile = outputFile,
                        password = if (method == EncryptionMethod.PASSWORD) password else null,
                        publicKeyPEM = if (method == EncryptionMethod.PUBLIC_KEY) publicKeyPem else null
                    )

                    lastEncryptedFile = outputFile // ✅ Guardar referencia para compartir

                    resultMessage = "✅ Cifrado exitoso:\n${outputFile.absolutePath}"
                    Toast.makeText(context, "Cifrado completado", Toast.LENGTH_SHORT).show()

                    openOutputFolder(context, outputFile.parentFile!!)

                } catch (e: Exception) {
                    resultMessage = "❌ Error: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🚀 CIFRAR")
        }

        if (resultMessage.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(resultMessage, style = MaterialTheme.typography.bodyMedium)
        }

// ✅ Botón de compartir
        if (lastEncryptedFile != null) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    lastEncryptedFile!!
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir archivo cifrado"))
            }) {
                Text("📤 Compartir archivo")
            }
        }

    }
}

