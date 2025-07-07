// ui/components/PemFilePicker.kt
package com.safeguard.encrypt_android.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun PemFilePicker(
    context: Context,
    filter: ((File) -> Boolean)? = null,
    onSelect: (fileName: String, pemContent: String) -> Unit
) {
    val llavesDir = File(context.filesDir, "Llaves").apply { mkdirs() }

    val pemFiles = llavesDir.listFiles()
        ?.filter { it.extension == "pem" }
        ?.filter { filter?.invoke(it) ?: true }
        ?: emptyList()

    Column(Modifier.fillMaxWidth()) {
        Text("🔐 Selecciona una llave", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (pemFiles.isEmpty()) {
            Text("⚠️ No hay llaves .pem disponibles.")
        } else {
            LazyColumn {
                items(pemFiles) { file ->
                    val isPrivate = remember(file) {
                        file.readText().contains("-----BEGIN RSA PRIVATE KEY-----")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val content = file.readText()
                                onSelect(file.name, content)
                            }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = if (isPrivate) Color(0xFFFFA000) else Color(0xFF00BCD4),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(file.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

