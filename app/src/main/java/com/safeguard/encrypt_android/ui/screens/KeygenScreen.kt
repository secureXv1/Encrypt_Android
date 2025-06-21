package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.safeguard.encrypt_android.crypto.KeyUtils
import java.io.File

@Composable
fun KeygenScreen() {
    val context = LocalContext.current
    var keyName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showKeyList by remember { mutableStateOf(false) }
    var pemFiles by remember { mutableStateOf(listOf<File>()) }

    Column(Modifier.padding(16.dp)) {
        Text("🔐 Crear Llaves RSA", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = keyName,
            onValueChange = { keyName = it },
            label = { Text("Nombre base para las llaves") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (keyName.isBlank()) {
                message = "⚠️ Debes ingresar un nombre."
                return@Button
            }

            try {
                val keyPair = KeyUtils.generateRSAKeyPair()

                val publicKeyBase64 = KeyUtils.encodePublicKeyToBase64(keyPair.public)
                val privateKeyBase64 = KeyUtils.encodePrivateKeyToBase64(keyPair.private)

                val publicPem = wrapAsPem(publicKeyBase64, "PUBLIC KEY")
                val privatePem = wrapAsPem(privateKeyBase64, "PRIVATE KEY")

                val pubFile = File(context.filesDir, "${keyName}_public.pem")
                val privFile = File(context.filesDir, "${keyName}_private.pem")

                pubFile.writeText(publicPem)
                privFile.writeText(privatePem)

                message = "✔️ Llaves guardadas como:\n${pubFile.name}\n${privFile.name}"
            } catch (e: Exception) {
                message = "❌ Error al generar llaves: ${e.message}"
            }
        }) {
            Text("Generar y guardar")
        }

        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            val allPem = context.filesDir.listFiles()?.filter {
                it.name.endsWith(".pem")
            } ?: emptyList()
            pemFiles = allPem.sortedBy { it.name }
            showKeyList = true
        }) {
            Text("📤 Compartir llaves")
        }

        if (showKeyList) {
            Spacer(Modifier.height(12.dp))
            Text("Selecciona una llave para compartir:", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                items(pemFiles) { file ->
                    Text(
                        text = "📄 ${file.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sharePemFile(context, file)
                            }
                            .padding(vertical = 6.dp)
                    )
                }
            }
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
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

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
