package com.safeguard.encrypt_android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeguard.endcrypt_android.R

@Composable
fun HomeScreen(userName: String = "User") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1B1E))
            .padding(horizontal = 16.dp)
    ) {
        // Encabezado superior
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.avatar_placeholder),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Green, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Bienvenido", color = Color.LightGray, fontSize = 14.sp)
                    Text(userName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = { /* Navegar a ajustes */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.settings),
                    contentDescription = "Ajustes",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Logo central
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Encrypt",
                modifier = Modifier.size(160.dp),
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = "Encrypt",
            color = Color(0xFF00FFD5),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tarjetas informativas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                title = "Características de seguridad",
                items = listOf(
                    "✅ Cifrado híbrido",
                    "🗂️ Ocultamiento de archivos dentro de otros",
                    "🛡️ Chat cifrado por túneles seguros"
                ),
                modifier = Modifier.weight(0.4f)
            )
            FeatureCard(
                title = "Consejo de seguridad del día",
                items = listOf(
                    "🔒 Nunca compartas tu clave privada.\nMantenla almacenada en un lugar seguro y fuera del dispositivo."
                ),
                modifier = Modifier.weight(0.6f)
            )
        }


        Spacer(modifier = Modifier.weight(1f))


    }
}

@Composable
fun FeatureCard(
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2B2D)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach {
                Text(
                    text = it,
                    color = Color(0xFFB0FDFD),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

