// ui/screens/EncryptScreen.kt
package com.safeguard.encrypt_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun EncryptScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        Text(
            "Operaciones de Cifrado",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        MenuButton("Crear Llaves", Icons.Default.VpnKey) {
            navController.navigate("keygen")
        }

        MenuButton("Cifrar Archivo", Icons.Default.Lock) {
            navController.navigate("encrypt_file")
        }

        MenuButton("Descifrar Archivo", Icons.Default.LockOpen) {
            navController.navigate("decrypt_file")
        }

        MenuButton("Ocultar Archivo Cifrado", Icons.Default.Image) {
            navController.navigate("hide_file")
        }

        MenuButton("Extraer Archivo Oculto", Icons.Default.Search) {
            navController.navigate("extract_hidden")
        }

        MenuButton("Extraer y Descifrar Oculto", Icons.Default.RemoveRedEye) {
            navController.navigate("extract_and_decrypt")
        }


    }
}

@Composable
fun MenuButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
            .background(Color(0xFF2C2C2C), shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00BCD4),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}
