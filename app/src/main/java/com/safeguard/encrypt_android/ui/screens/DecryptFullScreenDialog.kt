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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.safeguard.encrypt_android.crypto.CryptoController
import com.safeguard.encrypt_android.ui.components.PemFilePicker
import java.io.File

@Composable
fun DecryptFullScreenDialog(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("ninguno") }
    var password by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    var showPemPicker by remember { mutableStateOf(false) }
    var isJsonFile by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf<String?>(null) }

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

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Descifrar archivo") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Button(onClick = { pickFile.launch(arrayOf("*/*")) }) {
                    Text("📁 Seleccionar archivo")
                }

                Spacer(Modifier.height(8.dp))
                Text("Seleccionado: $selectedName")

                selectedUri?.let { uri ->
                    Spacer(Modifier.height(8.dp))

                    if (isJsonFile && type != null) {
                        if (type == "password") {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Contraseña") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))

                            Button(onClick = {
                                try {
                                    val input = context.contentResolver.openInputStream(uri)!!.readBytes()
                                    val temp = File.createTempFile("descifrar", ".json", context.cacheDir)
                                    temp.writeBytes(input)

                                    val decryptedFile = CryptoController.decrypt(
                                        inputFile = temp,
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
                        } else if (type == "rsa") {
                            Button(onClick = { showPemPicker = true }) {
                                Text("🔑 Descifrar con clave privada (.pem)")
                            }

                            if (showPemPicker) {
                                PemFilePicker(
                                    context = context,
                                    filter = { it.readText().contains("-----BEGIN RSA PRIVATE KEY-----") }
                                ) { _, pemContent ->
                                    try {
                                        val input = context.contentResolver.openInputStream(uri)!!.readBytes()
                                        val temp = File.createTempFile("descifrar", ".json", context.cacheDir)
                                        temp.writeBytes(input)

                                        val decryptedFile = CryptoController.decrypt(
                                            inputFile = temp,
                                            promptForPassword = { "" },
                                            privateKeyPEM = pemContent,
                                            allowAdminRecovery = true
                                        )

                                        val outDir = File(context.filesDir, "EncryptApp/Decrypted").apply { mkdirs() }
                                        decryptedFile.copyTo(File(outDir, decryptedFile.name), overwrite = true)

                                        resultMessage = "✅ Guardado: ${decryptedFile.name}"
                                        Toast.makeText(context, "Descifrado con clave", Toast.LENGTH_SHORT).show()
                                        onClose()
                                    } catch (e: Exception) {
                                        resultMessage = "❌ Error: ${e.message}"
                                    } finally {
                                        showPemPicker = false
                                    }
                                }
                            }
                        }
                    } else {
                        // Esteganografía
                        Button(onClick = {
                            try {
                                val raw = context.contentResolver.openInputStream(uri)!!.readBytes()
                                val parts = raw.toString(Charsets.ISO_8859_1).split("<<--BETTY_START-->>")
                                if (parts.size != 2) throw Exception("Contenido oculto no encontrado")

                                val extracted = parts[1].toByteArray(Charsets.ISO_8859_1)
                                val temp = File.createTempFile("descifrar", ".json", context.cacheDir)
                                temp.writeBytes(extracted)

                                val decryptedFile = CryptoController.decrypt(
                                    inputFile = temp,
                                    promptForPassword = { password },
                                    privateKeyPEM = null,
                                    allowAdminRecovery = true
                                )

                                val outDir = File(context.filesDir, "EncryptApp/Decrypted").apply { mkdirs() }
                                decryptedFile.copyTo(File(outDir, decryptedFile.name), overwrite = true)

                                resultMessage = "✅ Extraído y descifrado: ${decryptedFile.name}"
                                Toast.makeText(context, "Extraído y descifrado", Toast.LENGTH_SHORT).show()
                                onClose()
                            } catch (e: Exception) {
                                resultMessage = "❌ Error: ${e.message}"
                            }
                        }) {
                            Text("📤 Extraer y descifrar con contraseña")
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (resultMessage.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(resultMessage, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Cerrar")
            }
        }
    )
}
