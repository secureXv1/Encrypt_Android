package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.safeguard.encrypt_android.crypto.KeyUtils
import java.io.File

@Composable
fun KeygenScreen() {
    val context = LocalContext.current
    var pemFiles by remember { mutableStateOf(listOf<File>()) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showOverwriteDialog by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    // Importar archivo .pem
    var renameData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    fun getDisplayNameFromUri(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex != -1) it.getString(nameIndex) else null
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val inputBytes = context.contentResolver.openInputStream(uri)?.readBytes()
            val content = inputBytes?.toString(Charsets.UTF_8)

            if (content != null) {
                val type = when {
                    content.contains("-----BEGIN PUBLIC KEY-----") -> "public"
                    content.contains("-----BEGIN PRIVATE KEY-----") -> "private"
                    else -> {
                        Toast.makeText(context, "❌ El archivo no contiene una llave válida", Toast.LENGTH_LONG).show()
                        return@let
                    }
                }

                val fullName = getDisplayNameFromUri(context, uri) ?: "imported_key.pem"
                val originalName = fullName
                    .removeSuffix(".pem")
                    .removeSuffix("_public")
                    .removeSuffix("_private")

                renameData = Triple(originalName, type, content)
            } else {
                Toast.makeText(context, "❌ No se pudo leer el archivo", Toast.LENGTH_LONG).show()
            }
        }
    }

    renameData?.let { (defaultName, type, content) ->
        showRenameDialog(
            context = context,
            defaultName = defaultName,
            type = type,
            content = content,
            onSaved = { finalName ->
                val llavesDir = File(context.filesDir, "Llaves").apply { mkdirs() }
                val file = File(llavesDir, "${finalName}_${type}.pem")

                if (file.exists()) {
                    Toast.makeText(context, "⚠️ Ya existe una llave con ese nombre", Toast.LENGTH_LONG).show()
                } else {
                    file.writeText(content)
                    Toast.makeText(context, "✔️ Llave importada como: ${file.name}", Toast.LENGTH_SHORT).show()
                    val llavesDir = File(context.filesDir, "Llaves")
                    pemFiles = llavesDir.listFiles()?.filter { it.name.endsWith(".pem") }?.sortedBy { it.name } ?: emptyList()

                }
                renameData = null
            },
            onDismiss = {
                renameData = null
            }
        )
    }



    // Cargar llaves al iniciar
    LaunchedEffect(Unit) {
        val llavesDir = File(context.filesDir, "Llaves")
        pemFiles = llavesDir.listFiles()?.filter { it.name.endsWith(".pem") }?.sortedBy { it.name } ?: emptyList()

    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Encabezado: Crear / Importar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 16.dp)) {
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Crear", tint = Color.White)
                }
                Text("Crear", fontSize = 10.sp, color = Color.LightGray)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Importar", tint = Color.White)
                }
                Text("Importar", fontSize = 10.sp, color = Color.LightGray)
            }
        }

        // Diálogo para crear llave
        if (showCreateDialog) {
            CrearLlaveDialog(
                context = context,
                onLlaveCreada = {
                    showCreateDialog = false
                    pemFiles = context.filesDir.listFiles()?.filter { it.name.endsWith(".pem") }?.sortedBy { it.name } ?: emptyList()
                },
                onDismiss = { showCreateDialog = false },
                showOverwriteDialog = { showOverwriteDialog = it }
            )
        }

        // Diálogo de sobreescritura
        showOverwriteDialog?.let { (fileName, onConfirm) ->
            AlertDialog(
                onDismissRequest = { showOverwriteDialog = null },
                title = { Text("Archivo existente") },
                text = { Text("Ya existe una llave llamada $fileName. ¿Deseas sobrescribirla?") },
                confirmButton = {
                    TextButton(onClick = {
                        onConfirm()
                        showOverwriteDialog = null
                    }) {
                        Text("Sobrescribir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOverwriteDialog = null }) {
                        Text("Cancelar")
                    }
                },
                containerColor = Color(0xFF1E1E1E)
            )
        }

        // Campo de búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar llave") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Lista de llaves
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(pemFiles.filter { it.name.contains(searchQuery.text, ignoreCase = true) }) { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    elevation = CardDefaults.cardElevation(2.dp)
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
                            Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color(0xFF00BCD4))
                        }

                        IconButton(onClick = { fileToDelete = file }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                        }
                    }
                }
            }
        }

        // Confirmación de eliminación
        if (fileToDelete != null) {
            AlertDialog(
                onDismissRequest = { fileToDelete = null },
                title = { Text("¿Eliminar llave?", color = Color.White) },
                text = { Text("Esta acción eliminará permanentemente el archivo ${fileToDelete!!.name}.", color = Color.LightGray) },
                confirmButton = {
                    TextButton(onClick = {
                        fileToDelete!!.delete()
                        Toast.makeText(context, "Llave eliminada", Toast.LENGTH_SHORT).show()
                        pemFiles = pemFiles.filter { it != fileToDelete }
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

@Composable
fun CrearLlaveDialog(
    context: Context,
    onLlaveCreada: () -> Unit,
    onDismiss: () -> Unit,
    showOverwriteDialog: ((Pair<String, () -> Unit>) -> Unit)
) {
    var nombreLlave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear nueva llave") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombreLlave,
                    onValueChange = { nombreLlave = it },
                    label = { Text("Nombre de la llave") },
                    singleLine = true
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = Color.Red)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombreLlave.isBlank()) {
                    error = "El nombre no puede estar vacío"
                    return@TextButton
                }

                val llavesDir = File(context.filesDir, "Llaves").apply { mkdirs() }
                val pubFile = File(llavesDir, "${nombreLlave}_public.pem")
                val privFile = File(llavesDir, "${nombreLlave}_private.pem")

                val saveKeyPair: () -> Unit = {
                    val keyPair = KeyUtils.generateRSAKeyPair()
                    val pubPem = wrapAsPem(KeyUtils.encodePublicKeyToBase64(keyPair.public), "PUBLIC KEY")
                    val privPem = wrapAsPem(KeyUtils.encodePrivateKeyToBase64(keyPair.private), "PRIVATE KEY")
                    pubFile.writeText(pubPem)
                    privFile.writeText(privPem)
                    onLlaveCreada()
                }

                if (pubFile.exists() || privFile.exists()) {
                    showOverwriteDialog(pubFile.name to saveKeyPair)
                    onDismiss()
                } else {
                    saveKeyPair()
                    onDismiss()
                }
            }) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

@Composable
fun showRenameDialog(
    context: Context,
    defaultName: String,
    type: String,
    content: String,
    onSaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar llave") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la llave") },
                    singleLine = true
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = Color.Red)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    error = "El nombre no puede estar vacío"
                    return@TextButton
                }
                onSaved(name)
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}


