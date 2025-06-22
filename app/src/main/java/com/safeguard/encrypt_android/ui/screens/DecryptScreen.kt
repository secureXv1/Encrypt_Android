package com.safeguard.encrypt_android.ui.screens

import android.net.Uri
import android.os.Environment
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
import com.safeguard.encrypt_android.ui.components.PemFilePicker
import com.safeguard.encrypt_android.ui.screens.getFileNameWithExtensionFromUri
import java.io.File

@Composable
fun DecryptScreen() {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("ninguno") }
    var password by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    var showPemPicker by remember { mutableStateOf(false) }

    val pickFile = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        selectedUri = uri
        selectedName = uri?.let { getFileNameWithExtensionFromUri(context, it) } ?: "ninguno"
        resultMessage = ""
        password = ""
        showPemPicker = false
    }

    Column(Modifier.padding(16.dp)) {
        Text("🛠️ Extraer o Descifrar", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Button(onClick = { pickFile.launch(arrayOf("*/*")) }) {
            Text("📁 Seleccionar archivo")
        }

        Text("Archivo seleccionado: $selectedName")

        if (selectedUri != null) {
            val isJson = selectedName.endsWith(".json", ignoreCase = true)
            Spacer(Modifier.height(12.dp))

            if (isJson) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña (si aplica)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Button(onClick = {
                    try {
                        val input = context.contentResolver.openInputStream(selectedUri!!)!!.readBytes()
                        val temp = File.createTempFile("descifrar", ".json", context.cacheDir).apply {
                            writeBytes(input)
                        }

                        val outputFile = CryptoController.decrypt(
                            inputFile = temp,
                            promptForPassword = { password },
                            privateKeyPEM = null
                        )

                        resultMessage = "✅ Descifrado en: ${outputFile.absolutePath}"
                        Toast.makeText(context, "Archivo descifrado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        resultMessage = "❌ Error: ${e.message}"
                    }
                }) {
                    Text("🔓 Descifrar con contraseña")
                }

                Spacer(Modifier.height(8.dp))

                Button(onClick = { showPemPicker = !showPemPicker }) {
                    Text("🔑 Descifrar con clave privada (.pem)")
                }

                if (showPemPicker) {
                    PemFilePicker(context) { fileName, content ->
                        try {
                            val input = context.contentResolver.openInputStream(selectedUri!!)!!.readBytes()
                            val temp = File.createTempFile("descifrar", ".json", context.cacheDir).apply {
                                writeBytes(input)
                            }

                            val outputFile = CryptoController.decrypt(
                                inputFile = temp,
                                promptForPassword = { "" },
                                privateKeyPEM = content
                            )

                            resultMessage = "✅ Descifrado con clave: ${outputFile.absolutePath}"
                            Toast.makeText(context, "Archivo descifrado con clave", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            resultMessage = "❌ Error con clave: ${e.message}"
                        } finally {
                            showPemPicker = false
                        }
                    }
                }
            } else {
                // 📤 SOLO EXTRAER
                Button(onClick = {
                    try {
                        val raw = context.contentResolver.openInputStream(selectedUri!!)!!.readBytes()
                        val parts = raw.toString(Charsets.ISO_8859_1).split("<<--BETTY_START-->>")
                        if (parts.size != 2) throw Exception("❌ No se encontró contenido oculto")

                        val outputDir = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "Encrypt_Android"
                        ).apply { mkdirs() }

                        val extractedFile = File(outputDir, "extraido.json")
                        extractedFile.writeBytes(parts[1].toByteArray(Charsets.ISO_8859_1))

                        resultMessage = "✅ Extraído en: ${extractedFile.absolutePath}"
                        Toast.makeText(context, "Archivo extraído", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        resultMessage = "❌ Error al extraer: ${e.message}"
                    }
                }) {
                    Text("📤 Solo extraer")
                }

                Spacer(Modifier.height(12.dp))

                // 📤 EXTRAER Y DESCIFRAR CON CONTRASEÑA
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña para descifrar") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(onClick = {
                    try {
                        val raw = context.contentResolver.openInputStream(selectedUri!!)!!.readBytes()
                        val parts = raw.toString(Charsets.ISO_8859_1).split("<<--BETTY_START-->>")
                        if (parts.size != 2) throw Exception("❌ No se encontró contenido oculto")

                        val temp = File.createTempFile("extraido", ".json", context.cacheDir).apply {
                            writeBytes(parts[1].toByteArray(Charsets.ISO_8859_1))
                        }

                        val outputFile = CryptoController.decrypt(
                            inputFile = temp,
                            promptForPassword = { password },
                            privateKeyPEM = null
                        )

                        resultMessage = "✅ Extraído y descifrado: ${outputFile.absolutePath}"
                        Toast.makeText(context, "Extraído y descifrado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        resultMessage = "❌ Error al extraer y descifrar: ${e.message}"
                    }
                }) {
                    Text("📤 Extraer y descifrar con contraseña")
                }

                Spacer(Modifier.height(8.dp))

                Button(onClick = { showPemPicker = !showPemPicker }) {
                    Text("🔑 Extraer y descifrar con clave privada (.pem)")
                }

                if (showPemPicker) {
                    PemFilePicker(context) { fileName, content ->
                        try {
                            val raw = context.contentResolver.openInputStream(selectedUri!!)!!.readBytes()
                            val parts = raw.toString(Charsets.ISO_8859_1).split("<<--BETTY_START-->>")
                            if (parts.size != 2) throw Exception("❌ No se encontró contenido oculto")

                            val temp = File.createTempFile("extraido", ".json", context.cacheDir).apply {
                                writeBytes(parts[1].toByteArray(Charsets.ISO_8859_1))
                            }

                            val outputFile = CryptoController.decrypt(
                                inputFile = temp,
                                promptForPassword = { "" },
                                privateKeyPEM = content
                            )

                            resultMessage = "✅ Extraído y descifrado con clave: ${outputFile.absolutePath}"
                            Toast.makeText(context, "Extraído y descifrado con clave", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            resultMessage = "❌ Error con clave: ${e.message}"
                        } finally {
                            showPemPicker = false
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(resultMessage, style = MaterialTheme.typography.bodyMedium)
    }
}
