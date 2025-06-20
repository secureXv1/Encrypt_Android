package com.safeguard.encrypt_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.safeguard.endcrypt_android.R

@Composable
fun BottomNavBar(navController: NavController, currentRoute: String) {
    BottomNavigation(
        backgroundColor = Color(0xFF0E1B1E),
        contentColor = Color.White
    ) {
        BottomNavigationItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.home),
                    contentDescription = "Inicio"
                )
            },
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") }
        )
        BottomNavigationItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.lock),
                    contentDescription = "Encrypt",
                    tint = Color.Black,
                    modifier = Modifier
                        .background(Color(0xFF00FFD5), shape = CircleShape)
                        .padding(6.dp)
                )
            },
            selected = currentRoute == "encrypt",
            onClick = { navController.navigate("encrypt") }
        )
        BottomNavigationItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.tunnel),
                    contentDescription = "Túnel"
                )
            },
            selected = currentRoute == "tunnel",
            onClick = { navController.navigate("tunnel") }
        )
    }
}
