package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.safeguard.encrypt_android.utils.UuidUtils
import com.safeguard.encrypt_android.utils.getFileNameFromUri
import java.io.File
import java.util.*

@Composable
fun EncryptFullScreenDialog(
    context: Context,
    keyFiles: List<File>,
    onDismiss: () -> Unit,
    onSuccess: (File) -> Unit
) {
    val userUuid = UuidUtils.getClientUUID(context)

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var encryptionMethod by remember { mutableStateOf(Encryptor.Metodo.PASSWORD) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedKeyFile by remember { mutableStateOf<File?>(null) }
    var keySearch by remember { mutableStateOf("") }
    var isEncrypting by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedFileUri = uri
        selectedFileName = uri?.lastPathSegment?.substringAfterLast('/') ?: ""
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212).copy(alpha = 0.95f),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cifrado de archivo", color = Color.White, fontSize = 20.sp)
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = Color(0xFF2196F3))
                }
            }

            Button(
                onClick = { launcher.launch(arrayOf("*/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📁 Seleccionar archivo")
            }

            if (selectedFileName.isNotBlank()) {
                Text("Archivo: $selectedFileName", fontSize = 13.sp, color = Color.LightGray, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(16.dp))

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
                            }
                            .padding(vertical = 8.dp),
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
                OutlinedTextField(
                    value = keySearch,
                    onValueChange = { keySearch = it },
                    label = { Text("Buscar llave") },
                    modifier = Modifier.fillMaxWidth()
                )
                val filteredKeys = keyFiles.filter { it.name.contains(keySearch, ignoreCase = true) }
                Column(modifier = Modifier.heightIn(max = 200.dp)) {
                    filteredKeys.forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedKeyFile = file
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedKeyFile == file, onClick = { selectedKeyFile = file })
                            Text(text = file.name, color = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (selectedFileUri == null) {
                        Toast.makeText(context, "Seleccione un archivo", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (encryptionMethod == Encryptor.Metodo.PASSWORD && password != confirmPassword) {
                        Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
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
                    }
                    isEncrypting = false
                    if (resultFile != null) {
                        Toast.makeText(context, "Archivo cifrado exitosamente", Toast.LENGTH_SHORT).show()
                        onSuccess(resultFile)
                        onDismiss()
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
}

fun evaluarFortaleza(password: String): Pair<Int, String> {
    var nivel = 0
    if (password.length >= 8) nivel++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) nivel++
    if (password.any { it.isDigit() }) nivel++
    if (password.any { !it.isLetterOrDigit() }) nivel++

    return when (nivel) {
        0, 1 ->  Pair(25, "Débil")
        2     -> Pair(50, "Media")
        3, 4  -> Pair(100, "Fuerte")
        else  -> Pair(0, "Débil")
    }
}
