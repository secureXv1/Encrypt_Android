// ui/components/PemFilePicker.kt
package com.safeguard.encrypt_android.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun PemFilePicker(
    context: Context,
    onSelect: (fileName: String, pemContent: String) -> Unit
) {
    val pemFiles = remember {
        context.filesDir.listFiles()
            ?.filter { it.name.endsWith(".pem") }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    Column(Modifier.fillMaxWidth()) {
        Text("Selecciona una llave", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (pemFiles.isEmpty()) {
            Text("⚠️ No hay archivos .pem generados aún.")
        } else {
            LazyColumn {
                items(pemFiles) { file ->
                    Text(
                        text = file.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val content = file.readText()
                                onSelect(file.name, content)
                            }
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
