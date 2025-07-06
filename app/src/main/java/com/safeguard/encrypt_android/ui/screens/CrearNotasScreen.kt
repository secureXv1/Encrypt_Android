package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.Description
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearNotasScreen() {
    val context = LocalContext.current
    val notesDir = File(context.filesDir, "Notas").apply { mkdirs() }


    val keysDir = File(context.filesDir, "Llaves")
    val keyFiles = keysDir.listFiles()?.filter { it.extension == "pem" } ?: emptyList()




    var noteFiles by remember { mutableStateOf(emptyList<File>()) }
    var searchQuery by remember { mutableStateOf("") }
    var openFile by remember { mutableStateOf<File?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // NUEVOS estados
    var archivoSeleccionado by remember { mutableStateOf<File?>(null) }
    var llavesSeleccionadas by remember { mutableStateOf<List<File>>(emptyList()) }

    var showEncryptDialog by remember { mutableStateOf(false) }
    var notaEditando by remember { mutableStateOf<File?>(null) }
    val isEditing = notaEditando != null
    var showEncryptFromEdit by remember { mutableStateOf(false) }
    var postEncryptFile by remember { mutableStateOf<File?>(null) }




    LaunchedEffect(Unit) {
        noteFiles = notesDir.listFiles()
            ?.filter { it.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    val filteredNotes = noteFiles.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showCreateDialog) "Nueva Nota" else "Mis Notas",
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = {
                        if (isEditing) {
                            notaEditando = null
                        } else {
                            showCreateDialog = !showCreateDialog
                        }

                    }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF00BCD4), RoundedCornerShape(50))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (showCreateDialog || isEditing) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = if (showCreateDialog) "Cerrar" else "Nueva nota",
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
                .pointerInput(Unit) { detectTapGestures { openFile = null } }
                .padding(
                    top = padding.calculateTopPadding() + 12.dp,
                    start = padding.calculateStartPadding(LayoutDirection.Ltr),
                    end = padding.calculateEndPadding(LayoutDirection.Ltr),
                    bottom = padding.calculateBottomPadding()
                )
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar nota", color = Color.LightGray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = Color.Gray
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00BCD4),
                    unfocusedBorderColor = Color.DarkGray
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredNotes) { file ->
                    SwipeableNoteItem(
                        file = file,
                        isOpen = openFile == file,
                        onSetOpen = { selected -> openFile = if (selected) file else null },
                        onDelete = {
                            file.delete()
                            noteFiles = noteFiles.filter { it.exists() }
                        },
                        onEncrypt = {
                            archivoSeleccionado = file
                            showEncryptDialog = true
                        },
                        onEdit = { selected ->
                            notaEditando = selected
                        }

                    )

                    Divider(color = Color.DarkGray, thickness = 0.6.dp)
                }
            }
        }

        if (showCreateDialog) {
            CrearNotaFullScreenDialog(
                context = context,
                keyFiles = keyFiles, // ✅ PASAR LLAVES
                onDismiss = { showCreateDialog = false },
                onSave = { newFile ->
                    noteFiles = noteFiles + newFile
                    showCreateDialog = false
                },
                onEncrypt = { file, keys ->
                    noteFiles = noteFiles + file
                    archivoSeleccionado = file
                    llavesSeleccionadas = keys
                    showEncryptDialog = true
                    showCreateDialog = false
                }
            )


        }

        // MOSTRAR DIALOGO DE CIFRADO
        if (showEncryptDialog && archivoSeleccionado != null) {
            EncryptFullScreenDialog(
                context = context,
                initialFile = archivoSeleccionado,
                onDismiss = {
                    archivoSeleccionado = null
                    showEncryptDialog = false
                },
                onSuccess = { file ->
                    archivoSeleccionado = null
                    showEncryptDialog = false
                    postEncryptFile = file
                    noteFiles = noteFiles + file // Opcional, si deseas que se vea como nota cifrada
                }
            )

        }

        if (notaEditando != null) {
            EditarNotaDialog(
                context = context,
                nota = notaEditando!!,
                onDismiss = { notaEditando = null },
                onUpdated = { actualizada ->
                    noteFiles = noteFiles.map {
                        if (it.name == notaEditando!!.name) actualizada else it
                    }
                    notaEditando = null
                },
                onEncrypt = { nota ->
                    archivoSeleccionado = nota
                    showEncryptFromEdit = true
                    notaEditando = null
                }
            )
        }

        if (showEncryptFromEdit && archivoSeleccionado != null) {
            EncryptFullScreenDialog(
                context = context,
                initialFile = archivoSeleccionado,
                onDismiss = {
                    archivoSeleccionado = null
                    showEncryptFromEdit = false
                },
                onSuccess = { file ->
                    archivoSeleccionado = null
                    showEncryptFromEdit = false
                    postEncryptFile = file // ✅ Esto activa el PostEncryptOptionsDialog
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
}
