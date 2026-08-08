package com.amirovpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.amirovpn.ui.screen.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmiroTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun AmiroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val DarkColors = darkColorScheme(
        primary = Color(0xFF4FC3F7),
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF01579B),
        secondary = Color(0xFF81D4FA),
        background = Color.Black,
        surface = Color(0xFF0A0A0A),
        surfaceVariant = Color(0xFF1A1A1A),
        onBackground = Color.White,
        onSurface = Color.White
    )
    
    val LightColors = lightColorScheme(
        primary = Color(0xFF0277BD),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB3E5FC),
        secondary = Color(0xFF039BE5),
        background = Color(0xFFFAFAFA),
        surface = Color.White,
        surfaceVariant = Color(0xFFF0F0F0),
        onBackground = Color.Black,
        onSurface = Color.Black
    )
    
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}