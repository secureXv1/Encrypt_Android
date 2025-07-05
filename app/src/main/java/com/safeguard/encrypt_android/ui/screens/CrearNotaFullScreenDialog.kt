package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearNotaFullScreenDialog(
    context: Context,
    keyFiles: List<File>,
    onDismiss: () -> Unit,
    onSave: (File) -> Unit,
    onEncrypt: (File, List<File>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1B1E))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Spacer(Modifier.height(50.dp))

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
                    onEncrypt(file, keyFiles)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("Cifrar")
            }
        }
    }
}

fun saveNoteToFile(context: Context, title: String, content: String): File {
    val safeTitle = title.replace(Regex("[^a-zA-Z0-9_ -]"), "_")

    // Carpeta principal de notas
    val notasDir = File(context.filesDir, "Notas")
    notasDir.mkdirs()

    // Archivo final en la carpeta de notas
    val baseFile = File(notasDir, "$safeTitle.txt")
    var file = baseFile
    var counter = 1
    while (file.exists()) {
        file = File(notasDir, "$safeTitle ($counter).txt")
        counter++
    }

    // Escribe el contenido
    file.writeText(content)

    // Copia en Encrypt_Android/dat
    val copiaDir = File(context.filesDir, "Encrypt_Android/dat")
    copiaDir.mkdirs()

    val copiaFile = File(copiaDir, file.name)
    copiaFile.writeText(content)

    return file
}




@Composable
fun SwipeableNoteItem(
    file: File,
    isOpen: Boolean,
    onSetOpen: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEncrypt: () -> Unit,
    onEdit: (File) -> Unit
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
                .clickable { onEdit(file) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF00BCD4),
                        modifier = Modifier
                            .size(32.dp)
                            .padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            file.nameWithoutExtension,
                            color = Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val dateFormatted = remember(file) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            sdf.format(Date(file.lastModified()))
                        }
                        Text(
                            text = dateFormatted,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }

                Text(">", color = Color.LightGray, fontSize = 22.sp)
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

@Composable
fun EditarNotaDialog(
    context: Context,
    nota: File,
    onDismiss: () -> Unit,
    onUpdated: (File) -> Unit,
    onEncrypt: (File) -> Unit

) {
    var title by remember { mutableStateOf(nota.nameWithoutExtension) }
    var content by remember { mutableStateOf(TextFieldValue(nota.readText())) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1B1E))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Spacer(Modifier.height(50.dp))

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
                    if (title.isBlank()) {
                        Toast.makeText(context, "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val notasDir = File(context.filesDir, "Notas")
                    val newFile = File(notasDir, "$title.txt")
                    if (newFile != nota) {
                        nota.delete()
                    }
                    newFile.writeText(content.text)

                    val copiaDir = File(context.filesDir, "Encrypt_Android/dat")
                    val copiaFile = File(copiaDir, newFile.name)
                    copiaFile.writeText(content.text)

                    onUpdated(newFile)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Guardar cambios")
            }

            Button(
                onClick = {
                    val notasDir = File(context.filesDir, "Notas")
                    val newFile = File(notasDir, "$title.txt")
                    if (newFile != nota) {
                        nota.delete()
                    }
                    newFile.writeText(content.text)

                    val copiaDir = File(context.filesDir, "Encrypt_Android/dat")
                    val copiaFile = File(copiaDir, newFile.name)
                    copiaFile.writeText(content.text)

                    onEncrypt(newFile)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("Cifrar")
            }

        }

    }
}


