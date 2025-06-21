package com.safeguard.encrypt_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun TunnelSessionScreen(
    navController: NavController,
    tunnelId: String,
    alias: String
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Chat", "Participantes", "Archivos")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1B1E))
    ) {
        // Pestañas superiores
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color(0xFF1C2A2D),
            contentColor = Color(0xFF00BCD4),
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTabIndex == index) Color(0xFF00BCD4) else Color.White
                        )
                    }
                )
            }
        }

        // Contenido dinámico según pestaña seleccionada
        when (selectedTabIndex) {
            0 -> ChatTab(tunnelId = tunnelId, alias = alias)
            1 -> ParticipantsTab(tunnelId = tunnelId)
            2 -> FilesTab(tunnelId = tunnelId)
        }
    }
}
