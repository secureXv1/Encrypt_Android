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
import androidx.core.content.FileProvider
import com.safeguard.encrypt_android.crypto.CryptoController
import com.safeguard.encrypt_android.ui.components.PemFilePicker
import com.safeguard.encrypt_android.utils.getFileNameFromUri
import com.safeguard.encrypt_android.utils.openOutputFolder
import java.io.File
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun EncryptFileScreen() {
    val context = LocalContext.current
    var inputUri by remember { mutableStateOf<Uri?>(null) }
    var method by remember { mutableStateOf<Encryptor.Metodo?>(null) }
    var password by remember { mutableStateOf("") }
    var publicKeyPem by remember { mutableStateOf("") }
    var showPemPicker by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    var lastEncryptedFile by remember { mutableStateOf<File?>(null) }
    var isEncrypting by remember { mutableStateOf(false) }

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
                selected = method == Encryptor.Metodo.PASSWORD,
                onClick = {
                    method = Encryptor.Metodo.PASSWORD
                    showPemPicker = false
                }
            )
            Text("Contraseña", Modifier.padding(end = 16.dp))

            RadioButton(
                selected = method == Encryptor.Metodo.RSA,
                onClick = {
                    method = Encryptor.Metodo.RSA
                    password = ""
                }
            )
            Text("Llave pública")
        }

        if (method == Encryptor.Metodo.PASSWORD) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña segura") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }

        if (method == Encryptor.Metodo.RSA) {
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

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (inputUri == null || method == null) {
                    resultMessage = "⚠️ Selecciona archivo y método."
                    return@Button
                }

                isEncrypting = true
                resultMessage = ""

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val realName = getFileNameFromUri(context, inputUri!!) ?: "archivo.bin"
                        val inputFile = withContext(Dispatchers.IO) {
                            File(context.cacheDir, realName).apply {
                                context.contentResolver.openInputStream(inputUri!!)?.use {
                                    writeBytes(it.readBytes())
                                }
                            }
                        }


                        val encryptedFile = withContext(Dispatchers.IO) {
                            CryptoController.encrypt(
                                inputFile = inputFile,
                                method = method!!,
                                password = if (method == Encryptor.Metodo.PASSWORD) password else null,
                                publicKeyPEM = if (method == Encryptor.Metodo.RSA) publicKeyPem else null
                            )
                        }

                        lastEncryptedFile = encryptedFile
                        resultMessage = "✅ Cifrado exitoso:\n${encryptedFile.absolutePath}"
                        Toast.makeText(context, "Cifrado completado", Toast.LENGTH_SHORT).show()
                        openOutputFolder(context, encryptedFile.parentFile!!)

                    } catch (e: Exception) {
                        resultMessage = "❌ Error: ${e.message}"
                    } finally {
                        isEncrypting = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isEncrypting
        ) {
            if (isEncrypting) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
                Text("Cifrando...")
            } else {
                Text("🚀 CIFRAR")
            }
        }


        if (resultMessage.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(resultMessage, style = MaterialTheme.typography.bodyMedium)
        }

        if (lastEncryptedFile != null) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.safeguard.endcrypt_android.provider",
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
