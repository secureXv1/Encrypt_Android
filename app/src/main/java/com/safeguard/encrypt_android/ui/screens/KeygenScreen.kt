package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.safeguard.encrypt_android.crypto.KeyUtils
import java.io.File
import kotlin.math.roundToInt

@Composable
fun KeygenScreen() {
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showOverwriteDialog by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var keyPairs by remember { mutableStateOf<List<Pair<String, Pair<File?, File?>>>>(emptyList()) }


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
                    keyPairs = getKeyPairs(context)
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
        keyPairs = getKeyPairs(context)
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
                    keyPairs = getKeyPairs(context)
                },
                onDismiss = {
                    showCreateDialog = false
                },
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
            items(keyPairs.filter { it.first.contains(searchQuery.text, ignoreCase = true) }) { (name, pair) ->
                SwipeableKeyItem(
                    name = name,
                    publicKey = pair.first,
                    privateKey = pair.second,
                    context = context,
                    onDeleted = {
                        keyPairs = getKeyPairs(context)
                        Toast.makeText(context, "Llave eliminada", Toast.LENGTH_SHORT).show()
                    }
                )
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
                        keyPairs = getKeyPairs(context)
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
                    onLlaveCreada()
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

fun getKeyPairs(context: Context): List<Pair<String, Pair<File?, File?>>> {
    val dir = File(context.filesDir, "Llaves").apply { mkdirs() }
    val files = dir.listFiles()?.toList() ?: emptyList()

    val grouped = files.groupBy {
        it.name.removeSuffix("_public.pem").removeSuffix("_private.pem")
    }

    return grouped.map { (baseName, files) ->
        val public = files.find { it.name.endsWith("_public.pem") }
        val private = files.find { it.name.endsWith("_private.pem") }
        baseName to (public to private)
    }.sortedBy { it.first }
}

@Composable
fun SwipeableKeyItem(
    name: String,
    publicKey: File?,
    privateKey: File?,
    context: Context,
    onDeleted: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var showDialog by remember { mutableStateOf(false) }
    val maxOffset = with(LocalDensity.current) { 160.dp.toPx() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var confirmDeleteType by remember { mutableStateOf<String?>(null) }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(vertical = 4.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(-maxOffset, 0f)
                    },
                    onDragEnd = {
                        offsetX = if (offsetX < -maxOffset / 3) -maxOffset else 0f
                    }
                )
            }
    ) {
        // Fondo de acciones
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFF263238))
                .padding(end = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color(0xFF00BCD4))
            }
            IconButton(onClick = {
                showDeleteDialog = true
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
            }

        }

        // Contenido
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text(name, color = Color.White, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    if (publicKey != null) Text("🔐", fontSize = 16.sp)
                    if (privateKey != null) Text("🔒", fontSize = 16.sp)
                }

                Text(">", color = Color.LightGray, fontSize = 20.sp)
            }
        }

    }

    // Diálogo de compartir
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Compartir llave") },
            text = {
                Column {
                    if (publicKey != null) {
                        TextButton(onClick = {
                            sharePemFile(context, publicKey)
                            showDialog = false
                        }) {
                            Text("🔐 Pública: ${publicKey.name}")
                        }
                    }
                    if (privateKey != null) {
                        TextButton(onClick = {
                            sharePemFile(context, privateKey)
                            showDialog = false
                        }) {
                            Text("🔒 Privada: ${privateKey.name}")
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF1C1C1C)
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Qué deseas eliminar?", color = Color.White) },
            text = {
                Column {
                    if (publicKey != null) {
                        TextButton(onClick = {
                            confirmDeleteType = "public"
                            showDeleteDialog = false
                        }) {
                            Text("🔐 Llave pública", color = Color.White)
                        }
                    }
                    if (privateKey != null) {
                        TextButton(onClick = {
                            confirmDeleteType = "private"
                            showDeleteDialog = false
                        }) {
                            Text("🔒 Llave privada", color = Color.White)
                        }
                    }
                    if (publicKey != null && privateKey != null) {
                        TextButton(onClick = {
                            confirmDeleteType = "both"
                            showDeleteDialog = false
                        }) {
                            Text("🗑 Ambas", color = Color.Red)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF1C1C1C)
        )
    }
    if (confirmDeleteType != null) {
        val label = when (confirmDeleteType) {
            "public" -> "la llave pública"
            "private" -> "la llave privada"
            "both" -> "ambas llaves"
            else -> ""
        }

        AlertDialog(
            onDismissRequest = { confirmDeleteType = null },
            title = { Text("Confirmar eliminación", color = Color.White) },
            text = { Text("¿Estás seguro de que deseas eliminar $label?", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    when (confirmDeleteType) {
                        "public" -> publicKey?.delete()
                        "private" -> privateKey?.delete()
                        "both" -> {
                            publicKey?.delete()
                            privateKey?.delete()
                        }
                    }
                    confirmDeleteType = null
                    onDeleted()
                }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteType = null }) {
                    Text("Cancelar")
                }
            },
            containerColor = Color(0xFF1C1C1C)
        )
    }


}




