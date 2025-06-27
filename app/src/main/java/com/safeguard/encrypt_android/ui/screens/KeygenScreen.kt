package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.safeguard.encrypt_android.crypto.KeyUtils
import java.io.File

@Composable
fun KeygenScreen() {
    val context = LocalContext.current
    var keyName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var pemFiles by remember { mutableStateOf(listOf<File>()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Crear llave", Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = {
                selectedTab = 1
                val allPem = context.filesDir.listFiles()?.filter { it.name.endsWith(".pem") } ?: emptyList()
                pemFiles = allPem.sortedBy { it.name }
            }) {
                Text("Administrar llaves", Modifier.padding(12.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        when (selectedTab) {
            0 -> CrearLlaveUI(keyName, onKeyNameChange = { keyName = it }, onCreate = {
                if (keyName.isBlank()) {
                    message = "⚠️ Debes ingresar un nombre."
                    return@CrearLlaveUI
                }

                try {
                    val keyPair = KeyUtils.generateRSAKeyPair()

                    val pubPem = wrapAsPem(KeyUtils.encodePublicKeyToBase64(keyPair.public), "PUBLIC KEY")
                    val privPem = wrapAsPem(KeyUtils.encodePrivateKeyToBase64(keyPair.private), "PRIVATE KEY")

                    File(context.filesDir, "${keyName}_public.pem").writeText(pubPem)
                    File(context.filesDir, "${keyName}_private.pem").writeText(privPem)

                    message = "✔️ Llaves guardadas como: ${keyName}_public.pem y ${keyName}_private.pem"
                } catch (e: Exception) {
                    message = "❌ Error al generar llaves: ${e.message}"
                }
            })

            1 -> AdministrarLlavesUI(pemFiles, context)
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CrearLlaveUI(keyName: String, onKeyNameChange: (String) -> Unit, onCreate: () -> Unit) {
    Column {
        OutlinedTextField(
            value = keyName,
            onValueChange = onKeyNameChange,
            label = { Text("Nombre base para las llaves") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCreate, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Generar y guardar")
        }
    }
}

@Composable
fun AdministrarLlavesUI(pemFiles: List<File>, context: Context) {
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
        items(pemFiles) { file ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = file.name,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { sharePemFile(context, file) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = Color(0xFF00BCD4)
                        )
                    }

                    IconButton(onClick = { fileToDelete = file }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color.Red
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación al eliminar
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("¿Eliminar llave?", color = Color.White) },
            text = { Text("Esta acción eliminará permanentemente el archivo ${fileToDelete!!.name}.", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    fileToDelete!!.delete()
                    Toast.makeText(context, "Llave eliminada", Toast.LENGTH_SHORT).show()
                    fileToDelete = null
                }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancelar")
                }
            },
            containerColor = Color(0xFF1C1C1C)
        )
    }
}


fun wrapAsPem(base64: String, label: String): String {
    return buildString {
        appendLine("-----BEGIN $label-----")
        appendLine(base64.chunked(64).joinToString("\n"))
        appendLine("-----END $label-----")
    }
}

fun sharePemFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/x-pem-file"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir llave: ${file.name}"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error al compartir: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

