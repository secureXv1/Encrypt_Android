package com.safeguard.encrypt_android.ui.screens

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.safeguard.encrypt_android.crypto.CryptoController
import com.safeguard.encrypt_android.ui.components.PemFilePicker
import java.io.File

@Composable
fun DecryptFullScreenDialog(
    onClose: () -> Unit,
    initialFile: File? = null  // ⬅️ nuevo parámetro
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("ninguno") }
    var password by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    var showPemPicker by remember { mutableStateOf(false) }
    var isJsonFile by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf<String?>(null) }

    // Si hay archivo inicial, leerlo directamente
    val selectedFile = remember(initialFile) {
        initialFile
    }

    // Analizar tipo si es un archivo directo
    LaunchedEffect(selectedFile) {
        if (selectedFile != null) {
            val content = selectedFile.readText()
            isJsonFile = content.trim().startsWith("{")
            if (isJsonFile) {
                type = Regex("\"type\"\\s*:\\s*\"(password|rsa)\"").find(content)?.groupValues?.get(1)
                selectedName = selectedFile.name
            }
        }
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedUri = uri
        selectedName = uri?.lastPathSegment ?: "ninguno"
        password = ""
        resultMessage = ""
        showPemPicker = false
        type = null

        uri?.let {
            val input = context.contentResolver.openInputStream(it)?.readBytes()
            if (input != null) {
                val content = String(input)
                isJsonFile = content.trim().startsWith("{")
                if (isJsonFile) {
                    type = Regex("\"type\"\\s*:\\s*\"(password|rsa)\"").find(content)?.groupValues?.get(1)
                }
            }
        }
    }

    // 🔓 UI
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Descifrar archivo") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (initialFile == null) {
                    Button(onClick = { pickFile.launch(arrayOf("*/*")) }) {
                        Text("📁 Seleccionar archivo")
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Seleccionado: $selectedName")

                val fileToDecrypt = selectedFile ?: selectedUri?.let {
                    val bytes = context.contentResolver.openInputStream(it)?.readBytes() ?: return@let null
                    val temp = File.createTempFile("descifrar", ".json", context.cacheDir).apply {
                        writeBytes(bytes)
                    }
                    temp
                }

                fileToDecrypt?.let { file ->
                    if (isJsonFile && type != null) {
                        when (type) {
                            "password" -> {
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Contraseña") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = {
                                    try {
                                        val decryptedFile = CryptoController.decrypt(
                                            inputFile = file,
                                            promptForPassword = { password },
                                            privateKeyPEM = null,
                                            allowAdminRecovery = true
                                        )
                                        val outDir = File(context.filesDir, "EncryptApp/Decrypted").apply { mkdirs() }
                                        decryptedFile.copyTo(File(outDir, decryptedFile.name), overwrite = true)
                                        resultMessage = "✅ Guardado: ${decryptedFile.name}"
                                        Toast.makeText(context, "Archivo descifrado", Toast.LENGTH_SHORT).show()
                                        onClose()
                                    } catch (e: Exception) {
                                        resultMessage = "❌ Error: ${e.message}"
                                    }
                                }) {
                                    Text("🔓 Descifrar con contraseña")
                                }
                            }

                            "rsa" -> {
                                Button(onClick = { showPemPicker = true }) {
                                    Text("🔑 Descifrar con clave privada (.pem)")
                                }

                                if (showPemPicker) {
                                    PemFilePicker(
                                        context = context,
                                        filter = { it.readText().contains("-----BEGIN RSA PRIVATE KEY-----") }
                                    ) { _, pemContent ->
                                        try {
                                            val decryptedFile = CryptoController.decrypt(
                                                inputFile = file,
                                                promptForPassword = { "" },
                                                privateKeyPEM = pemContent,
                                                allowAdminRecovery = true
                                            )
                                            val outDir = File(context.filesDir, "EncryptApp/Decrypted").apply { mkdirs() }
                                            decryptedFile.copyTo(File(outDir, decryptedFile.name), overwrite = true)
                                            resultMessage = "✅ Descifrado: ${decryptedFile.name}"
                                            Toast.makeText(context, "Archivo descifrado", Toast.LENGTH_SHORT).show()
                                            onClose()
                                        } catch (e: Exception) {
                                            resultMessage = "❌ Error: ${e.message}"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (resultMessage.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(resultMessage)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Cerrar")
            }
        },
        containerColor = Color(0xFF1A1A1A)
    )
}

