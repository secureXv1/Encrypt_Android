package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearNotaFullScreenDialog(
    context: Context,
    onDismiss: () -> Unit,
    onSave: (File) -> Unit,
    onEncrypt: (File) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Nota", color = Color.White) },
                actions = {
                    IconButton(onClick = onDismiss) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color = Color(0xFF00BCD4), shape = RoundedCornerShape(50))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1B1E)),
                modifier = Modifier.height(52.dp)
            )
        },
        containerColor = Color(0xFF0F1B1E)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00BCD4),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Contenido") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00BCD4),
                    unfocusedBorderColor = Color.Gray
                ),
                maxLines = Int.MAX_VALUE
            )

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = {
                        if (title.isBlank() || content.text.isBlank()) {
                            Toast.makeText(context, "Debe ingresar título y contenido", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val file = saveNoteToFile(context, title, content.text)
                        onSave(file)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Guardar")
                }
                Button(
                    onClick = {
                        if (title.isBlank() || content.text.isBlank()) {
                            Toast.makeText(context, "Debe ingresar título y contenido", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val file = saveNoteToFile(context, title, content.text)
                        onEncrypt(file)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Cifrar")
                }
            }
        }
    }
}

fun saveNoteToFile(context: Context, title: String, content: String): File {
    val safeTitle = title.replace(Regex("[^a-zA-Z0-9_ -]"), "_")
    val finalDir = File(context.filesDir, "Notas")
    finalDir.mkdirs()

    val baseFile = File(finalDir, "$safeTitle.txt")
    var file = baseFile
    var counter = 1
    while (file.exists()) {
        file = File(finalDir, "$safeTitle ($counter).txt")
        counter++
    }
    file.writeText(content)
    return file
}

@Composable
fun SwipeableNoteItem(
    file: File,
    isOpen: Boolean,
    onSetOpen: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEncrypt: () -> Unit
) {
    val buttonWidth = 80.dp
    val swipeThresholdPx = with(LocalDensity.current) { (buttonWidth * 2).toPx() }
    var offsetX by remember { mutableStateOf(0f) }
    var showConfirmDialog by remember { mutableStateOf(false) }

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
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF263238)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionButton(
                    icon = Icons.Default.Lock,
                    backgroundColor = Color(0xFF00BCD4),
                    onClick = onEncrypt
                )
                ActionButton(
                    icon = Icons.Default.Delete,
                    backgroundColor = Color.Red,
                    onClick = { showConfirmDialog = true }
                )
            }
        }

        // Contenido deslizable
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxSize()
                .background(Color(0xFF1E2A2D), shape = RoundedCornerShape(12.dp))
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(file.nameWithoutExtension, color = Color.White, fontSize = 16.sp)
                Text(
                    "📝 Nota TXT",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("¿Eliminar nota?") },
            text = { Text("¿Seguro que deseas eliminar \"${file.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onDelete()
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

