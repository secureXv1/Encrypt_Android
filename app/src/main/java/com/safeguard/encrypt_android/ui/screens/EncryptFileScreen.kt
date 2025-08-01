package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.safeguard.encrypt_android.utils.UuidUtils
import com.safeguard.encrypt_android.utils.getFileNameFromUri
import java.io.File
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.filled.FileDownload
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import org.json.JSONObject
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import com.safeguard.encrypt_android.ui.screens.EncryptFullScreenDialog




@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material.ExperimentalMaterialApi::class
)
@Composable
fun EncryptFileScreen() {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var encryptedFiles by remember { mutableStateOf(listOf<File>()) }
    var showFullDialog by remember { mutableStateOf(false) } // ⬅️ Nuevo

    val encryptDir = File(context.filesDir, "EncryptApp")
    var openFile by remember { mutableStateOf<File?>(null) }
    var infoFile by remember { mutableStateOf<File?>(null) }

    var postEncryptFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        if (encryptDir.exists()) {
            encryptedFiles = encryptDir.listFiles()
                ?.filter { it.extension == "json" }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        }
    }

    val filteredFiles = encryptedFiles.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archivos Cifrados", color = Color.White) },
                actions = {
                    IconButton(onClick = { showFullDialog = true }) {
                        if (!showFullDialog) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(color = Color(0xFF00BCD4), shape = RoundedCornerShape(50))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Nuevo archivo",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1B1E)),
                modifier = Modifier.height(52.dp) // altura compacta
            )
        },
        containerColor = Color(0xFF0F1B1E)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(
                    top = padding.calculateTopPadding() + 12.dp, // ⬅️ agrega separación real
                    start = padding.calculateStartPadding(LayoutDirection.Ltr),
                    end = padding.calculateEndPadding(LayoutDirection.Ltr),
                    bottom = padding.calculateBottomPadding()
                )
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Buscar archivo",
                        color = Color.LightGray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00BCD4),
                    unfocusedBorderColor = Color.DarkGray,
                    unfocusedLabelColor = Color.Gray
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )





            Spacer(Modifier.height(12.dp))


            // 📂 Lista de archivos
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredFiles) { file ->
                    SwipeableFileItemV2(
                        file = file,
                        isOpen = openFile == file,
                        onSetOpen = { selected -> openFile = if (selected) file else null },
                        onDownload = {
                            val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Encrypt_Android")
                            downloadDir.mkdirs()
                            val outFile = File(downloadDir, file.name)
                            outFile.writeText(file.readText())
                            Toast.makeText(context, "✔️ Archivo descargado", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteConfirmed = {
                            file.delete()
                            encryptedFiles = encryptedFiles.filter { it.exists() }
                        },
                        onShare = {
                            postEncryptFile = file
                        }
                        ,
                        onInfoClick = { infoFile = file }
                    )
                    Divider(color = Color.DarkGray, thickness = 0.6.dp)
                }
            }
        }

        infoFile?.let { file ->
            InfoDialog(file = file, onDismiss = { infoFile = null })
        }



    }
    // ⬅️ Diálogo full screen
    if (showFullDialog) {
        EncryptFullScreenDialog(
            context = context,
            onDismiss = { showFullDialog = false },
            onSuccess = { file ->
                showFullDialog = false
                postEncryptFile = file
                encryptedFiles = encryptedFiles + file
            }
        )
    }

    if (postEncryptFile != null) {
        PostEncryptOptionsDialog(
            encryptedFile = postEncryptFile!!,
            onDismiss = { postEncryptFile = null }
        )
    }


}


@Composable
fun FileItemStyled(file: File) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF1E2A2D))
        .padding(horizontal = 16.dp, vertical = 12.dp)) {

        Text(file.name, color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            "📅 Modificado: ${java.text.SimpleDateFormat("dd/MM/yyyy, HH:mm").format(file.lastModified())}",
            color = Color.LightGray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun FileItemSwipeable(
    file: File,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onEncrypt: () -> Unit
) {
    val share = SwipeAction(
        icon = {
            Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color.Cyan)
        },
        background = Color(0xFF37474F),
        onSwipe = onShare
    )

    val delete = SwipeAction(
        icon = {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White)
        },
        background = Color.Red,
        onSwipe = onDelete
    )

    val encrypt = SwipeAction(
        icon = {
            Icon(Icons.Default.Lock, contentDescription = "Cifrar", tint = Color.White)
        },
        background = Color(0xFF00BCD4),
        onSwipe = onEncrypt
    )

    SwipeableActionsBox(
        endActions = listOf(encrypt, delete, share),
        swipeThreshold = 100.dp
    ) {
        FileItemStyled(file)
    }
}


@Composable
fun SwipeableFileItemV2(
    file: File,
    isOpen: Boolean,
    onSetOpen: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onShare: () -> Unit,
    onInfoClick: () -> Unit
) {
    val buttonWidth = 80.dp
    val swipeThresholdPx = with(LocalDensity.current) { (buttonWidth * 3).toPx() }
    var offsetX by remember { mutableStateOf(0f) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(isOpen) {
        offsetX = if (isOpen) -swipeThresholdPx else 0f
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(65.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(-swipeThresholdPx, 0f)
                    },
                    onDragEnd = {
                        val shouldOpen = offsetX < -swipeThresholdPx / 4
                        onSetOpen(shouldOpen)
                    }
                )
            }
    ) {
        // Fondo de acciones
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(vertical = 6.dp, horizontal = 6.dp)
                .clip(RoundedCornerShape(12.dp)) // igual que la tarjeta
                .background(Color(0xFF263238)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButton(
                    icon = Icons.Default.FileDownload,
                    backgroundColor = Color(0xFF00BCD4),
                    onClick = onDownload
                )
                ActionButton(
                    icon = Icons.Default.Delete,
                    backgroundColor = Color(0xFFF44336),
                    onClick = { showConfirmDialog = true }
                )
                ActionButton(
                    icon = Icons.Default.Share,
                    backgroundColor = Color(0xFFFF9800),
                    onClick = onShare
                )
            }
        }


        // Contenido deslizable con estilo moderno
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxSize()
                .clickable { onInfoClick() }
                .background(
                    color = Color(0xFF1E2A2D),
                    shape = RoundedCornerShape(12.dp)
                )
                .shadow(4.dp, RoundedCornerShape(12.dp)) // Sombra sutil
                .padding(horizontal = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Image(
                    painter = painterResource(id = context.resources.getIdentifier("encrypt", "drawable", context.packageName)),
                    contentDescription = "Archivo cifrado",
                    modifier = Modifier
                        .size(42.dp)
                        .padding(end = 12.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f) // ⬅️ el texto ocupa espacio limitado
                ) {
                    Text(
                        file.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis // ⬅️ para cortar texto largo
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault()).format(file.lastModified()),
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = ">",
                    color = Color.LightGray,
                    fontSize = 22.sp
                )
            }
        }

    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("¿Eliminar archivo?") },
            text = { Text("¿Seguro que deseas eliminar \"${file.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onDeleteConfirmed()
                }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}


@Composable
fun ActionButton(
    icon: ImageVector,
    backgroundColor: Color,
    iconTint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
    }
}

@Composable
fun InfoDialog(
    file: File,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showDecryptDialog by remember { mutableStateOf(false) }

    val tipoCrudo = try {
        val json = JSONObject(file.readText()).optString("type", "desconocido")
        json.lowercase()
    } catch (e: Exception) {
        "desconocido"
    }

    val tipo = when (tipoCrudo) {
        "password" -> "Contraseña"
        "rsa" -> "Llave"
        else -> "Desconocido"
    }

    val fecha = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault()).format(file.lastModified())
    val pesoKb = file.length() / 1024

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Información del archivo", color = Color.White) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFFC107))
                    Spacer(Modifier.width(8.dp))
                    Text(file.name, color = Color.White, fontSize = 14.sp)
                }

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFFE91E63))
                    Spacer(Modifier.width(8.dp))
                    Text("Fecha de modificación: $fecha", color = Color.LightGray, fontSize = 13.sp)
                }

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00BCD4))
                    Spacer(Modifier.width(8.dp))
                    Text("Tipo de cifrado: $tipo", color = Color.LightGray, fontSize = 13.sp)
                }

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF795548))
                    Spacer(Modifier.width(8.dp))
                    Text("Tamaño: $pesoKb KB", color = Color.LightGray, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = Color.LightGray)
                }
                TextButton(
                    onClick = { showDecryptDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00BCD4))
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Descifrar")
                }
            }
        },
        containerColor = Color(0xFF1A1A1A)
    )

    if (showDecryptDialog) {
        DecryptFullScreenDialog(
            initialFile = file,
            onClose = { showDecryptDialog = false }
        )
    }
}




