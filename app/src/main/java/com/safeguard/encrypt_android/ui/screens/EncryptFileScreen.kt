package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DismissDirection
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.rememberDismissState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.FileDownload


@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material.ExperimentalMaterialApi::class
)
@Composable
fun EncryptFileScreen() {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var encryptedFiles by remember { mutableStateOf(listOf<File>()) }
    var showDialog by remember { mutableStateOf(false) }

    val encryptDir = File(context.filesDir, "EncryptApp")

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

    if (showDialog) {
        EncryptFileDialog(
            context = context,
            keyFiles = context.filesDir.listFiles()?.filter { it.name.endsWith("_public.pem") } ?: emptyList(),
            onDismiss = { showDialog = false },
            onSuccess = { file ->
                encryptedFiles = encryptedFiles + file
                showDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archivos Cifrados", color = Color.White) },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Text("➕", fontSize = 22.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F1B1E))
            )
        },
        containerColor = Color(0xFF0F1B1E)
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar archivo") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00BCD4),
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredFiles) { file ->
                    val dismissState = rememberDismissState()

                    SwipeToDismiss(
                        state = dismissState,
                        directions = setOf(DismissDirection.EndToStart),
                        dismissThresholds = { FractionalThreshold(0.25f) },
                        background = {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF263238))
                                    .padding(end = 12.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Compartir archivo"))
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color.Cyan)
                                }

                                IconButton(onClick = {
                                    val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Encrypt_Android")
                                    downloadDir.mkdirs()
                                    val outFile = File(downloadDir, file.name)
                                    outFile.writeText(file.readText())
                                    Toast.makeText(context, "✔️ Archivo descargado", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.FileDownload, contentDescription = "Descargar", tint = Color.White)
                                }

                                IconButton(onClick = {
                                    file.delete()
                                    encryptedFiles = encryptedFiles.filter { it.exists() }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                                }
                            }
                        },
                        dismissContent = {
                            FileItemStyled(file)
                        }
                    )
                    Divider(color = Color.DarkGray, thickness = 0.6.dp)
                }
            }
        }
    }
}

@Composable
fun FileItemStyled(file: File) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF1E2A2D))
        .padding(horizontal = 16.dp, vertical = 12.dp)) {

        Text(file.name, color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "📅 Modificado: ${java.text.SimpleDateFormat("dd/MM/yyyy, HH:mm").format(file.lastModified())}",
            color = Color.LightGray,
            fontSize = 12.sp
        )
    }
}





@Composable
fun EncryptFileDialog(
            context: Context,
            keyFiles: List<File>,
            onDismiss: () -> Unit,
            onSuccess: (File) -> Unit
        ) {
            val userUuid = UuidUtils.getClientUUID(context)

            var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
            var selectedFileName by remember { mutableStateOf("") }
            var encryptionMethod by remember { mutableStateOf<Encryptor.Metodo?>(null) }
            var password by remember { mutableStateOf("") }
            var selectedKeyFile by remember { mutableStateOf<File?>(null) }
            var isEncrypting by remember { mutableStateOf(false) }

            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                selectedFileUri = uri
                selectedFileName = uri?.lastPathSegment?.substringAfterLast('/') ?: ""
            }

            AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {},
                title = { Text("Nuevo archivo a cifrar") },
                text = {
                    Column {
                        OutlinedButton(
                            onClick = { launcher.launch(arrayOf("*/*")) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("📁 Seleccionar archivo")
                        }

                        if (selectedFileName.isNotBlank()) {
                            Text("Archivo: $selectedFileName", fontSize = 13.sp, color = Color.Gray)
                        }

                        Spacer(Modifier.height(12.dp))

                        Text("Método de cifrado:", color = Color.Black)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = encryptionMethod == Encryptor.Metodo.PASSWORD,
                                onClick = { encryptionMethod = Encryptor.Metodo.PASSWORD }
                            )
                            Text("Contraseña")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = encryptionMethod == Encryptor.Metodo.RSA,
                                onClick = { encryptionMethod = Encryptor.Metodo.RSA }
                            )
                            Text("Llave pública")
                        }

                        if (encryptionMethod == Encryptor.Metodo.PASSWORD) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Contraseña") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (encryptionMethod == Encryptor.Metodo.RSA) {
                            var expanded by remember { mutableStateOf(false) }

                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(selectedKeyFile?.name ?: "Elegir llave pública")
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    keyFiles.forEach { file ->
                                        DropdownMenuItem(
                                            text = { Text(file.name) },
                                            onClick = {
                                                selectedKeyFile = file
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (selectedFileUri == null || encryptionMethod == null) {
                                    Toast.makeText(context, "Faltan datos", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val inputStream = context.contentResolver.openInputStream(selectedFileUri!!)
                                val fileBytes = inputStream?.readBytes()
                                val fileName = getFileNameFromUri(context, selectedFileUri!!)

                                if (fileBytes == null || fileName == null) {
                                    Toast.makeText(context, "No se pudo leer el archivo", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val originalFile = File(context.cacheDir, fileName).apply { writeBytes(fileBytes) }

                                isEncrypting = true

                                val resultFile = when (encryptionMethod) {
                                    Encryptor.Metodo.PASSWORD -> Encryptor.encryptWithPassword(originalFile, password, userUuid)
                                    Encryptor.Metodo.RSA -> {
                                        val pem = selectedKeyFile?.readText()
                                        if (pem != null) Encryptor.encryptWithPublicKey(originalFile, pem, userUuid)
                                        else null
                                    }
                                    else -> null
                                }

                                isEncrypting = false

                                if (resultFile != null) {
                                    Toast.makeText(context, "Archivo cifrado exitosamente", Toast.LENGTH_SHORT).show()
                                    onSuccess(resultFile)
                                    onDismiss()
                                }
                            },
                            enabled = !isEncrypting
                        ) {
                            Text("CIFRAR")
                        }
                    }
                }
            )
        }





