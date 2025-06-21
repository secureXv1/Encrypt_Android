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
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.safeguard.encrypt_android.ui.screens.DecryptScreen
import com.safeguard.encrypt_android.ui.screens.EncryptFileScreen
import com.safeguard.endcrypt_android.R
import com.safeguard.encrypt_android.ui.screens.HomeScreen
import com.safeguard.encrypt_android.ui.screens.EncryptScreen
import com.safeguard.encrypt_android.ui.screens.ExtractAndDecryptScreen
import com.safeguard.encrypt_android.ui.screens.ExtractHiddenFileScreen
import com.safeguard.encrypt_android.ui.screens.HideFileScreen
import com.safeguard.encrypt_android.ui.screens.KeygenScreen
import com.safeguard.encrypt_android.ui.screens.TunnelScreen
import com.safeguard.encrypt_android.ui.screens.TunnelSessionScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = Color(0xFF0E1B1E),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0E1B1E)) {
                val currentDestination = navController
                    .currentBackStackEntryAsState().value?.destination?.route

                NavigationBarItem(
                    icon = {
                        if (currentDestination == "home") {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF00FFD5), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(id = R.drawable.home),
                                    contentDescription = "Inicio",
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            Icon(
                                painterResource(id = R.drawable.home),
                                contentDescription = "Inicio",
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                        }
                    },
                    selected = currentDestination == "home",
                    onClick = { navController.navigate("home") }
                )

                NavigationBarItem(
                    icon = {
                        if (currentDestination == "encrypt") {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
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
                        } else {
                            Icon(
                                painterResource(id = R.drawable.lock),
                                contentDescription = "Encrypt",
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                        }
                    },
                    selected = currentDestination == "encrypt",
                    onClick = { navController.navigate("encrypt") }
                )

                NavigationBarItem(
                    icon = {
                        if (currentDestination == "tunnel") {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF00FFD5), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(id = R.drawable.tunnel),
                                    contentDescription = "Túneles",
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            Icon(
                                painterResource(id = R.drawable.tunnel),
                                contentDescription = "Túneles",
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                        }
                    },
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
            composable("encrypt") { EncryptScreen(navController) }
            composable("tunnel") { TunnelScreen(navController) }

            composable("keygen") { KeygenScreen() }
            composable("encrypt_file") { EncryptFileScreen() }
            composable("decrypt_file") { DecryptScreen() }
            composable("hide_file") { HideFileScreen() }
            composable("extract_hidden") { ExtractHiddenFileScreen() }
            composable("extract_and_decrypt") { ExtractAndDecryptScreen() }

            composable(
                route = "tunnel_session/{tunnelId}/{alias}",
                arguments = listOf(
                    navArgument("tunnelId") { type = NavType.StringType },
                    navArgument("alias") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val tunnelId = backStackEntry.arguments?.getString("tunnelId") ?: ""
                val alias = backStackEntry.arguments?.getString("alias") ?: ""
                TunnelSessionScreen(navController = navController, tunnelId = tunnelId, alias = alias)
            }
        }

    }

}
