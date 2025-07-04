package com.safeguard.encrypt_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Create





enum class EncryptMenuOption(val title: String, val icon: ImageVector) {
    KEYS("Llaves", Icons.Default.VpnKey),
    CREATE("Crear", Icons.Default.Menu),
    ENCRYPT("Cifrar/Ocultar", Icons.Default.Lock),
    DECRYPT("Descifrar/Extraer", Icons.Default.LockOpen)
}

@Composable
fun EncryptScreen() {
    var selected by remember { mutableStateOf(EncryptMenuOption.ENCRYPT) }

    Row(modifier = Modifier.fillMaxSize()) {

        // Menú lateral fijo (solo íconos y texto abajo)
        Column(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .background(Color(0xFF2C2C2C)),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            EncryptMenuOption.values().forEach { option ->
                val isSelected = selected == option
                MenuIconTileCompact(
                    title = option.title,
                    icon = option.icon,
                    selected = isSelected
                ) {
                    selected = option
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Contenido principal a la derecha
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            when (selected) {
                EncryptMenuOption.KEYS -> KeygenScreen()
                EncryptMenuOption.CREATE -> CrearNotasScreen()
                EncryptMenuOption.ENCRYPT -> EncryptFileScreen()
                EncryptMenuOption.DECRYPT -> DecryptScreen()
            }
        }
    }
}



@Composable
fun MenuIconTileCompact(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) Color(0xFF00BCD4) else Color(0xFF1E1E1E)
    val contentColor = if (selected) Color.Black else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(bgColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title.take(6), // si quieres limitarlo visualmente
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}


