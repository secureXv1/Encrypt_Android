package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.safeguard.encrypt_android.utils.UuidUtils
import com.safeguard.encrypt_android.utils.getFileNameFromUri
import com.safeguard.encrypt_android.utils.shareFile
import java.io.File
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptFullScreenDialog(
    context: Context,
    initialFile: File? = null,
    onDismiss: () -> Unit,
    onSuccess: (File) -> Unit
) {
    val userUuid = UuidUtils.getClientUUID(context)

    var selectedFile by remember { mutableStateOf(initialFile) }
    var selectedFileName by remember { mutableStateOf(initialFile?.name ?: "") }
    var encryptionMethod by remember { mutableStateOf(Encryptor.Metodo.PASSWORD) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedKeyFile by remember { mutableStateOf<File?>(null) }
    var isEncrypting by remember { mutableStateOf(false) }
    val llavesDir = File(context.filesDir, "Llaves").apply { mkdirs() }
    val keyFiles = remember {
        llavesDir.listFiles()
            ?.filter { it.extension == "pem" && it.readText().contains("-----BEGIN PUBLIC KEY-----") }
            ?: emptyList()
    }

    var mostrarPostDialog by remember { mutableStateOf(false) }
    var archivoRecienCifrado by remember { mutableStateOf<File?>(null) }



    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri)
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileBytes = inputStream?.readBytes()
            if (fileBytes != null && fileName != null) {
                val tempFile = File(context.cacheDir, generateUniqueFileName(context.cacheDir, fileName))
                tempFile.writeBytes(fileBytes)
                selectedFile = tempFile
                selectedFileName = fileName
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cifrar Archivo", color = Color.White) },
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
            if (initialFile == null) {
                Button(
                    onClick = { launcher.launch(arrayOf("*/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📁 Seleccionar archivo")
                }
            }

            if (selectedFileName.isNotBlank()) {
                Text("Archivo: $selectedFileName", fontSize = 13.sp, color = Color.LightGray, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Método de cifrado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.DarkGray),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val selectedColor = Color(0xFF2196F3)
                val unselectedColor = Color.LightGray

                listOf("Contraseña", "Llave pública").forEachIndexed { index, label ->
                    val selected = (index == 0 && encryptionMethod == Encryptor.Metodo.PASSWORD) ||
                            (index == 1 && encryptionMethod == Encryptor.Metodo.RSA)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (selected) selectedColor else Color.Transparent)
                            .clickable {
                                encryptionMethod = if (index == 0) Encryptor.Metodo.PASSWORD else Encryptor.Metodo.RSA
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else unselectedColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (encryptionMethod == Encryptor.Metodo.PASSWORD) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(icon, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contraseña") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (password != confirmPassword && confirmPassword.isNotBlank()) {
                    Text("❗ Las contraseñas no coinciden", color = Color.Red, fontSize = 12.sp)
                }

                val (fortaleza, etiquetaFortaleza) = evaluarFortaleza(password)
                val animatedProgress by animateFloatAsState(targetValue = fortaleza / 100f)
                val colorFortaleza = when (etiquetaFortaleza) {
                    "Débil" -> Color.Red
                    "Media" -> Color(0xFFFFC107)
                    "Fuerte" -> Color(0xFF4CAF50)
                    else -> Color.Gray
                }

                if (password.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = colorFortaleza,
                        trackColor = Color.DarkGray
                    )
                    Text(
                        text = "Fortaleza: $etiquetaFortaleza",
                        color = colorFortaleza,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (encryptionMethod == Encryptor.Metodo.RSA) {
                Spacer(Modifier.height(12.dp))
                KeySelectionSection(
                    keyFiles = keyFiles,
                    selectedKey = selectedKeyFile,
                    onKeySelected = { selectedKeyFile = it }
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (selectedFile == null) {
                        Toast.makeText(context, "Seleccione un archivo", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (encryptionMethod == Encryptor.Metodo.PASSWORD && password != confirmPassword) {
                        Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isEncrypting = true
                    val resultFile = when (encryptionMethod) {
                        Encryptor.Metodo.PASSWORD -> Encryptor.encryptWithPassword(selectedFile!!, password, userUuid)
                        Encryptor.Metodo.RSA -> {
                            val pem = selectedKeyFile?.readText()
                            if (pem != null) Encryptor.encryptWithPublicKey(selectedFile!!, pem, userUuid)
                            else null
                        }
                    }
                    isEncrypting = false

                    if (resultFile != null && resultFile.exists()) {
                        Toast.makeText(context, "Archivo cifrado exitosamente", Toast.LENGTH_SHORT).show()

                        archivoRecienCifrado = resultFile
                        mostrarPostDialog = true

                        onSuccess(resultFile) // Ya fue generado correctamente por el Encryptor
                    }




                },
                enabled = !isEncrypting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CIFRAR")
            }





        }
    }
    if (mostrarPostDialog && archivoRecienCifrado != null) {
        PostEncryptOptionsDialog(
            encryptedFile = archivoRecienCifrado!!,
            onDismiss = {
                mostrarPostDialog = false
                archivoRecienCifrado = null
            }
        )
    }
}



fun evaluarFortaleza(password: String): Pair<Int, String> {
    var nivel = 0
    if (password.length >= 8) nivel++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) nivel++
    if (password.any { it.isDigit() }) nivel++
    if (password.any { !it.isLetterOrDigit() }) nivel++

    return when (nivel) {
        0, 1 -> Pair(25, "Débil")
        2 -> Pair(50, "Media")
        3, 4 -> Pair(100, "Fuerte")
        else -> Pair(0, "Débil")
    }
}

@Composable
fun KeySelectionSection(
    keyFiles: List<File>,
    selectedKey: File?,
    onKeySelected: (File) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredKeys = remember(searchQuery, keyFiles) {
        keyFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar llave") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00BCD4),
                unfocusedBorderColor = Color.Gray
            )
        )

        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
            items(filteredKeys) { key ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onKeySelected(key) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedKey == key) Color(0xFF00BCD4).copy(alpha = 0.2f) else Color(0xFF1E2A2D)
                    ),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = key.name,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

fun generateUniqueFileName(directory: File, originalName: String): String {
    val baseName = originalName.substringBeforeLast(".")
    val extension = originalName.substringAfterLast(".", "")
    var fileName = originalName
    var counter = 1

    while (File(directory, fileName).exists()) {
        fileName = if (extension.isNotEmpty()) {
            "$baseName ($counter).$extension"
        } else {
            "$baseName ($counter)"
        }
        counter++
    }

    return fileName
}






@Composable
fun PostEncryptOptionsDialog(encryptedFile: File, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var seleccionarContenedor by remember { mutableStateOf(false) }

    if (seleccionarContenedor) {
        FilePickerForHiding(encryptedFile = encryptedFile) {
            seleccionarContenedor = false
            onDismiss()
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✔️ Archivo cifrado") },
        text = { Text("¿Deseas ocultarlo dentro de una imagen o PDF antes de compartir?") },
        confirmButton = {
            TextButton(onClick = {
                seleccionarContenedor = true
            }) {
                Text("Sí, ocultar")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                shareFile(context, encryptedFile)
                onDismiss()
            }) {
                Text("No, solo compartir")
            }
        },
        containerColor = Color(0xFF1A1A1A)
    )
}





