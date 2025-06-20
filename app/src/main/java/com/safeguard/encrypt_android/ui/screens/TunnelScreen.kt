package com.safeguard.encrypt_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TunnelScreen() {
    var isCreatingTunnel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1B1E))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isCreatingTunnel) "Crear Túnel" else "Conectarse a Túnel",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        TunnelForm(isCreating = isCreatingTunnel)

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = { isCreatingTunnel = !isCreatingTunnel }) {
            Text(
                text = if (isCreatingTunnel) "¿Ya tienes un túnel? Conéctate" else "¿No tienes túnel? Crear uno",
                color = Color(0xFF00BCD4)
            )
        }
    }
}

@Composable
fun TunnelForm(isCreating: Boolean) {
    var tunnelName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = tunnelName,
            onValueChange = { tunnelName = it },
            label = { Text("Nombre del túnel") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = inputColors()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = inputColors()
        )

        if (!isCreating) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                label = { Text("Tu alias") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = inputColors()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                mensaje = if (isCreating)
                    "Intentando crear túnel '$tunnelName'..."
                else
                    "Intentando conectar a '$tunnelName' como '$alias'..."
                // Lógica real se implementará luego
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
        ) {
            Text(text = if (isCreating) "Crear túnel" else "Conectarse", color = Color.Black)
        }

        if (mensaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = mensaje, color = Color.LightGray)
        }
    }
}

@Composable
fun inputColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF00BCD4),
    unfocusedBorderColor = Color.Gray,
    focusedLabelColor = Color(0xFF00BCD4),
    cursorColor = Color(0xFF00BCD4),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

