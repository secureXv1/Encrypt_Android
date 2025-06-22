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
import androidx.core.content.FileProvider
import com.safeguard.encrypt_android.crypto.CryptoController
import com.safeguard.encrypt_android.ui.components.PemFilePicker
import com.safeguard.encrypt_android.utils.getFileNameFromUri
import java.io.File
import kotlinx.coroutines.*

@Composable
fun EncryptFileScreen() {
    val context = LocalContext.current
    var inputUri by remember { mutableStateOf<Uri?>(null) }
    var method by remember { mutableStateOf<Encryptor.Metodo?>(null) }
    var password by remember { mutableStateOf("") }
    var publicKeyPem by remember { mutableStateOf("") }
    var showPemPicker by remember { mutableStateOf(false) }

    var encryptedFile by remember { mutableStateOf<File?>(null) }
    var hiddenFile by remember { mutableStateOf<File?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }

    val pickInputFile = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        inputUri = uri
    }

    val pickContainerFile = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null && encryptedFile != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val contBytes = context.contentResolver.openInputStream(uri)!!.readBytes()
                    val cifBytes = encryptedFile!!.readBytes()
                    val delimiter = "<<--BETTY_START-->>".toByteArray()

                    val containerName = getFileNameFromUri(context, uri) ?: "oculto.bin"
                    val outputDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "Encrypt_Android"
                    ).apply { mkdirs() }

                    val result = File(outputDir, containerName)
                    result.writeBytes(contBytes + delimiter + cifBytes)

                    withContext(Dispatchers.Main) {
                        hiddenFile = result
                        Toast.makeText(context, "✅ Archivo oculto creado", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "❌ Error al ocultar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("🔐 Cifrar Archivo", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Button(onClick = { pickInputFile.launch(arrayOf("*/*")) }) {
            Text("📁 Seleccionar archivo")
        }

        inputUri?.let {
            Text("Seleccionado: ${getFileNameFromUri(context, it)}", Modifier.padding(top = 6.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text("Método de cifrado:", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = method == Encryptor.Metodo.PASSWORD, onClick = {
                method = Encryptor.Metodo.PASSWORD
                showPemPicker = false
            })
            Text("Contraseña", Modifier.padding(end = 16.dp))
            RadioButton(selected = method == Encryptor.Metodo.RSA, onClick = {
                method = Encryptor.Metodo.RSA
                password = ""
            })
            Text("Llave pública")
        }

        if (method == Encryptor.Metodo.PASSWORD) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }

        if (method == Encryptor.Metodo.RSA) {
            Button(onClick = { showPemPicker = !showPemPicker }) {
                Text("🔑 Seleccionar llave pública")
            }
            if (publicKeyPem.isNotBlank()) {
                Text("✔️ Llave cargada correctamente", Modifier.padding(top = 6.dp))
            }
            if (showPemPicker) {
                PemFilePicker(context) { fileName, content ->
                    publicKeyPem = content
                    showPemPicker = false
                    Toast.makeText(context, "Llave $fileName cargada", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (inputUri == null || method == null) {
                    Toast.makeText(context, "⚠️ Selecciona archivo y método", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isProcessing = true
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val name = getFileNameFromUri(context, inputUri!!) ?: "archivo.json"
                        val inputFile = File(context.cacheDir, name).apply {
                            context.contentResolver.openInputStream(inputUri!!)?.use {
                                writeBytes(it.readBytes())
                            }
                        }

                        val result = CryptoController.encrypt(
                            inputFile,
                            method!!,
                            if (method == Encryptor.Metodo.PASSWORD) password else null,
                            if (method == Encryptor.Metodo.RSA) publicKeyPem else null
                        )

                        withContext(Dispatchers.Main) {
                            val publicDir = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                "Encrypt_Android"
                            ).apply { mkdirs() }

                            val cleanName = result.name.replace(" ", "_")
                            val publicEncrypted = File(publicDir, cleanName)

                            result.copyTo(publicEncrypted, overwrite = true)

                            encryptedFile = publicEncrypted

                            resultMessage = "✅ Cifrado completado"
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            resultMessage = "❌ Error: ${e.message}"
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            isProcessing = false
                        }
                    }
                }
            },
            enabled = !isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isProcessing) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Procesando...")
            } else {
                Text("🚀 CIFRAR")
            }
        }

        Spacer(Modifier.height(20.dp))

        encryptedFile?.let {
            FilePreview(it.name, it) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.safeguard.endcrypt_android.provider",
                    it
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir archivo cifrado"))
            }

            Spacer(Modifier.height(8.dp))
            Button(onClick = { pickContainerFile.launch(arrayOf("*/*")) }) {
                Text("🖼️ Ocultar en contenedor")
            }
        }

        hiddenFile?.let {
            Spacer(Modifier.height(16.dp))
            FilePreview(it.name, it) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.safeguard.endcrypt_android.provider",
                    it
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir archivo oculto"))
            }
        }

        Spacer(Modifier.height(20.dp))
        if (resultMessage.isNotBlank()) {
            Text(resultMessage, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun FilePreview(fileName: String, file: File, onShare: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Text("📄", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
        Spacer(Modifier.width(10.dp))
        Text(fileName, modifier = Modifier.weight(1f))
        IconButton(onClick = onShare) {
            Text("📤", fontSize = MaterialTheme.typography.titleLarge.fontSize)
        }
    }
}
