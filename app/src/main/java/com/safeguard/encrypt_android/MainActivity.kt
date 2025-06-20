package com.safeguard.encrypt_android

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.safeguard.encrypt_android.ui.theme.Encrypt_AndroidTheme
import com.safeguard.encrypt_android.MainScreen
import com.safeguard.encrypt_android.data.ClientService
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registrarClienteSiEsPrimeraVez()

        enableEdgeToEdge()
        setContent {
            Encrypt_AndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    private fun registrarClienteSiEsPrimeraVez() {
        val prefs = getSharedPreferences("securex_prefs", Context.MODE_PRIVATE)

        val uuid = prefs.getString("uuid", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("uuid", it).apply()
        }

        val registrado = prefs.getBoolean("cliente_registrado", false)
        if (registrado) {
            Log.i("Cliente", "Ya registrado con UUID: $uuid")
            return
        }

        val hostname = Build.MODEL
        val sistema = "Android ${Build.VERSION.RELEASE}"

        ClientService.registrarCliente(uuid) { success, error ->
            if (success) {
                prefs.edit().putBoolean("cliente_registrado", true).apply()
                Log.i("Cliente", "✅ Cliente registrado con UUID: $uuid")
            } else {
                Log.e("Cliente", "❌ Error registrando cliente: $error")
            }
        }
    }
}
