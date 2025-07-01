package com.safeguard.encrypt_android.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.safeguard.encrypt_android.crypto.CryptoController
import com.safeguard.encrypt_android.ui.components.PemFilePicker
import com.safeguard.encrypt_android.utils.getFileNameFromUri
import java.io.File
import kotlinx.coroutines.*

@Composable
fun EncryptFileScreen() {
    val context = LocalContext.current
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }

    var encryptionMethod by remember { mutableStateOf<Encryptor.Metodo?>(null) }
    var password by remember { mutableStateOf("") }

    var keyFiles by remember { mutableStateOf(listOf<File>()) }
    var selectedKeyFile by remember { mutableStateOf<File?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedFileUri = uri
        selectedFileName = uri?.lastPathSegment?.substringAfterLast('/') ?: ""
    }

    LaunchedEffect(Unit) {
        keyFiles = context.filesDir.listFiles()?.filter { it.name.endsWith("_public.pem") }?.sortedBy { it.name } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1B1E))
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔐 Cifrar Archivo", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { launcher.launch(arrayOf("*/*")) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("📁 Seleccionar archivo a cifrar", color = Color.Black)
        }

        if (selectedFileName.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text("Seleccionado: $selectedFileName", color = Color.LightGray, fontSize = 13.sp)
        }

        Spacer(Modifier.height(28.dp))
        Text("Método de cifrado:", color = Color.White)
        Spacer(Modifier.height(12.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = encryptionMethod == Encryptor.Metodo.PASSWORD,
                    onClick = { encryptionMethod = Encryptor.Metodo.PASSWORD }
                )
                Text("🔒 Contraseña", color = Color.White)
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = encryptionMethod == Encryptor.Metodo.RSA,
                    onClick = { encryptionMethod = Encryptor.Metodo.RSA }
                )
                Text("🔑 Llave pública", color = Color.White)
            }
        }

        Spacer(Modifier.height(20.dp))

        when (encryptionMethod) {
            Encryptor.Metodo.PASSWORD -> {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00BCD4),
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }

            Encryptor.Metodo.RSA -> {
                Spacer(Modifier.height(8.dp))
                Text("Seleccionar llave pública:", color = Color.White)
                Spacer(Modifier.height(6.dp))

                if (keyFiles.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(selectedKeyFile?.name ?: "Elegir llave")
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF2C2C2C))
                        ) {
                            keyFiles.forEach { file ->
                                DropdownMenuItem(
                                    text = { Text(file.name, color = Color.White) },
                                    onClick = {
                                        selectedKeyFile = file
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text("⚠️ No se encontraron llaves públicas", color = Color.Red)
                }
            }

            null -> {}
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (selectedFileUri == null) {
                    Toast.makeText(context, "⚠️ Debes seleccionar un archivo", Toast.LENGTH_SHORT).show()
                } else if (encryptionMethod == null) {
                    Toast.makeText(context, "❌ Método de cifrado no seleccionado", Toast.LENGTH_SHORT).show()
                } else {
                    val inputStream = context.contentResolver.openInputStream(selectedFileUri!!)
                    val fileBytes = inputStream?.readBytes()
                    val fileName = getFileNameFromUri(context, selectedFileUri!!)

                    if (fileBytes == null || fileName == null) {
                        Toast.makeText(context, "❌ No se pudo leer el archivo", Toast.LENGTH_SHORT).show()
                    } else {
                        val originalFile = File(context.cacheDir, fileName).apply { writeBytes(fileBytes) }

                        val resultFile = when (encryptionMethod) {
                            Encryptor.Metodo.PASSWORD -> {
                                if (password.isBlank()) {
                                    Toast.makeText(context, "⚠️ Ingresa una contraseña", Toast.LENGTH_SHORT).show()
                                    null
                                } else {
                                    try {
                                        Encryptor.encryptWithPassword(originalFile, password)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "❌ Error al cifrar: ${e.message}", Toast.LENGTH_LONG).show()
                                        null
                                    }
                                }
                            }

                            Encryptor.Metodo.RSA -> {
                                if (selectedKeyFile == null) {
                                    Toast.makeText(context, "⚠️ Selecciona una llave pública", Toast.LENGTH_SHORT).show()
                                    null
                                } else {
                                    try {
                                        val pem = selectedKeyFile!!.readText()
                                        Encryptor.encryptWithPublicKey(originalFile, pem)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "❌ Error al cifrar: ${e.message}", Toast.LENGTH_LONG).show()
                                        null
                                    }
                                }
                            }

                            else -> null
                        }

                        if (resultFile != null) {
                            // Guardar también en la carpeta pública
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val outputDir = File(downloadsDir, "Encrypt_Android").apply { mkdirs() }
                            val outputFile = File(outputDir, resultFile.name)
                            outputFile.writeText(resultFile.readText())

                            Toast.makeText(context, "✔️ Archivo cifrado guardado en: ${outputFile.name}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            enabled = selectedFileUri != null && encryptionMethod != null,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Cifrar", tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            Text("CIFRAR", color = Color.Black)
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
