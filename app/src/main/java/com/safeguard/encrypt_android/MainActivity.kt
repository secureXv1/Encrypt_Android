package com.safeguard.encrypt_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.safeguard.endcrypt_android.ui.HomeScreen
import com.safeguard.encrypt_android.ui.theme.Encrypt_AndroidTheme
import com.safeguard.endcrypt_android.ui.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Encrypt_AndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(userName = "User") // 👈 Aquí usamos la pantalla real
                }
            }
        }
    }
}


