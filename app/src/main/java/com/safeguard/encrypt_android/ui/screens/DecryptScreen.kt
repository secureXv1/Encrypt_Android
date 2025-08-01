@file:Suppress("DEPRECATION")

package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.safeguard.encrypt_android.crypto.CryptoController
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archivos Descifrados", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1B1E)),
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        containerColor = Color(0xFF0F1B1E),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFF00BCD4),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo descifrado")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {

            if (archivos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay archivos descifrados", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(archivos, key = { it.name }) { archivo ->
                        DecryptedFileItem(
                            file = archivo,
                            onDelete = {
                                archivo.delete()
                                archivos = listarArchivos(decryptedDir)
                            },
                            onShare = { compartirArchivo(context, archivo) },
                            onDownload = { descargarArchivo(context, archivo) }
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
}

@Composable
fun DecryptedFileItem(
    file: File,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    val ext = file.extension.lowercase()
    val context = LocalContext.current

    val iconName = when (ext) {
        "pdf" -> "file_pdf"
        "png", "jpg", "jpeg" -> "file_png"
        "doc", "docx" -> "file_docx"
        "xls", "xlsx" -> "file_xlsx"
        "ppt", "pptx" -> "file_ppt"
        "mp3" -> "file_mp3"
        "mp4" -> "file_mp4"
        "zip" -> "file_zip"
        "rar" -> "file_rar"
        else -> "file_default"
    }

    val iconId = remember(iconName) {
        context.resources.getIdentifier(iconName, "drawable", context.packageName)
    }

    val fallbackId = remember {
        context.resources.getIdentifier("file_default", "drawable", context.packageName)
    }

    val finalIconId = if (iconId != 0) iconId else fallbackId

    val share = SwipeAction(
        icon = { Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color.Cyan) },
        background = Color(0xFF37474F),
        onSwipe = onShare
    )

    val delete = SwipeAction(
        icon = { Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White) },
        background = Color.Red,
        onSwipe = onDelete
    )

    val download = SwipeAction(
        icon = { Icon(Icons.Default.Download, contentDescription = "Descargar", tint = Color.White) },
        background = Color(0xFF00BCD4),
        onSwipe = onDownload
    )

    SwipeableActionsBox(
        endActions = listOf(download, share, delete),
        swipeThreshold = 100.dp
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2A2D))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = finalIconId),
                    contentDescription = "Icono de archivo",
                    modifier = Modifier.size(40.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(file.name, color = Color.White, fontSize = 15.sp, maxLines = 1)
                    Text(
                        text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(file.lastModified()),
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
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
