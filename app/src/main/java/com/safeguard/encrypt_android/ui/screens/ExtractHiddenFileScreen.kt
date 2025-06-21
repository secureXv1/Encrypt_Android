// ui/screens/ExtractHiddenFileScreen.kt
package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import com.safeguard.encrypt_android.utils.indexOfFirstSlice


@Composable
fun ExtractHiddenFileScreen() {
    val context = LocalContext.current
    var containerUri by remember { mutableStateOf<Uri?>(null) }
    var resultMessage by remember { mutableStateOf("") }

    val pickContainer = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        containerUri = uri
    }

    Column(Modifier.padding(16.dp)) {
        Text("🔍 Extraer Archivo Oculto", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(12.dp))
        Button(onClick = { pickContainer.launch(arrayOf("*/*")) }) {
            Text("Seleccionar archivo contenedor")
        }

        Text("Contenedor: ${containerUri?.lastPathSegment ?: "ninguno"}")

        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            if (containerUri == null) {
                resultMessage = "⚠️ Selecciona un archivo contenedor."
                return@Button
            }

            try {
                val contStream = context.contentResolver.openInputStream(containerUri!!)!!
                val content = contStream.readBytes()
                val delimiter = "<<--BETTY_START-->>".toByteArray()
                val index = content.indexOfFirstSlice(delimiter)

                if (index == -1) {
                    resultMessage = "❌ No se encontró un archivo oculto en el contenedor."
                    return@Button
                }

                val extractedData = content.copyOfRange(index + delimiter.size, content.size)
                val outputFile = File(context.cacheDir, "extraido.json")
                outputFile.writeBytes(extractedData)

                resultMessage = "✅ Archivo extraído:\n${outputFile.absolutePath}"
                Toast.makeText(context, "Archivo extraído correctamente", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                resultMessage = "❌ Error: ${e.message}"
            }
        }) {
            Text("Extraer")
        }

        Spacer(Modifier.height(16.dp))
        Text(resultMessage, style = MaterialTheme.typography.bodyMedium)
    }
}
