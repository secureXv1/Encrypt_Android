package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.safeguard.encrypt_android.crypto.CryptoController
import com.safeguard.encrypt_android.ui.components.PemFilePicker
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecryptScreen() {
    val context = LocalContext.current
    val decryptedDir = File(context.filesDir, "EncryptApp/Decrypted").apply { mkdirs() }
    var archivos by remember { mutableStateOf(listarArchivos(decryptedDir)) }
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Archivos Descifrados") },
            actions = {
                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo descifrado")
                }
            }
        )

        if (archivos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay archivos descifrados")
            }
        } else {
            LazyColumn {
                items(archivos, key = { it.name }) { archivo ->
                    FileCardItem(
                        file = archivo,
                        onDelete = {
                            archivo.delete()
                            archivos = listarArchivos(decryptedDir)
                        },
                        onShare = {
                            compartirArchivo(context, archivo)
                        },
                        onDownload = {
                            descargarArchivo(context, archivo)
                        }
                    )
                }
            }
        }

        if (showDialog) {
            DecryptFullScreenDialog(
                onClose = {
                    showDialog = false
                    archivos = listarArchivos(decryptedDir)
                }
            )
        }
    }
}

@Composable
fun FileCardItem(
    file: File,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)) {
        ListItem(
            overlineContent = null,
            headlineContent = { Text(file.name) },
            supportingContent = {
                Text(
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(file.lastModified())
                )
            },
            trailingContent = {
                Row {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = "Descargar")
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        )

    }
}

fun listarArchivos(dir: File): List<File> =
    dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()

fun compartirArchivo(context: Context, archivo: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", archivo)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir archivo"))
}

fun descargarArchivo(context: Context, archivo: File) {
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val destino = File(downloadsDir, archivo.name)
    archivo.copyTo(destino, overwrite = true)
    Toast.makeText(context, "Guardado en Descargas", Toast.LENGTH_SHORT).show()
}
