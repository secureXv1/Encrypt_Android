// ui/screens/KeygenScreen.kt
package com.safeguard.encrypt_android.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.safeguard.encrypt_android.crypto.KeyUtils
import java.io.File

@Composable
fun KeygenScreen() {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {
        Text("🔐 Crear Llaves RSA", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            try {
                val keyPair = KeyUtils.generateRSAKeyPair()

                val publicKeyBase64 = KeyUtils.encodePublicKeyToBase64(keyPair.public)
                val privateKeyBase64 = KeyUtils.encodePrivateKeyToBase64(keyPair.private)

                val publicPem = wrapAsPem(publicKeyBase64, "PUBLIC KEY")
                val privatePem = wrapAsPem(privateKeyBase64, "PRIVATE KEY")

                val pubFile = File(context.filesDir, "public.pem")
                val privFile = File(context.filesDir, "private.pem")

                pubFile.writeText(publicPem)
                privFile.writeText(privatePem)

                message = "Llaves generadas correctamente:\n\n" +
                        "• public.pem\n• private.pem"

                Toast.makeText(context, "✔️ Llaves guardadas en almacenamiento interno", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                message = "Error al generar llaves: ${e.message}"
            }
        }) {
            Text("Generar y guardar llaves")
        }

        Spacer(Modifier.height(24.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

fun wrapAsPem(base64: String, label: String): String {
    return buildString {
        appendLine("-----BEGIN $label-----")
        appendLine(base64.chunked(64).joinToString("\n"))
        appendLine("-----END $label-----")
    }
}
