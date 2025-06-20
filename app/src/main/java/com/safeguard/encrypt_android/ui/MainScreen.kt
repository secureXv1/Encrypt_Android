package com.safeguard.encrypt_android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.safeguard.endcrypt_android.R
import com.safeguard.encrypt_android.ui.screens.HomeScreen
import com.safeguard.encrypt_android.ui.screens.EncryptScreen
import com.safeguard.encrypt_android.ui.screens.TunnelScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = Color(0xFF0E1B1E),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0E1B1E)) {
                val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

                NavigationBarItem(
                    icon = { Icon(painterResource(id = R.drawable.home), contentDescription = "Inicio") },
                    selected = currentDestination == "home",
                    onClick = { navController.navigate("home") }
                )

                NavigationBarItem(
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF00FFD5), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(id = R.drawable.lock),
                                contentDescription = "Encrypt",
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    selected = currentDestination == "encrypt",
                    onClick = { navController.navigate("encrypt") }
                )

                NavigationBarItem(
                    icon = { Icon(painterResource(id = R.drawable.tunnel), contentDescription = "Túneles") },
                    selected = currentDestination == "tunnel",
                    onClick = { navController.navigate("tunnel") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen() }
            composable("encrypt") { EncryptScreen() }
            composable("tunnel") { TunnelScreen() }
        }
    }
}
