package com.amiro.vpn.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amiro.vpn.core.AmiroVpnService
import com.amiro.vpn.core.ServerScanner
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var isConnected by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf("انتخاب خودکار") }
    var latency by remember { mutableStateOf(0L) }
    var speed by remember { mutableStateOf(0.0) }
    val scope = rememberCoroutineScope()

    val gradientColors = listOf(
        Color(0xFF1a1a2e),
        Color(0xFF16213e),
        Color(0xFF0f3460)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(gradientColors))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Âmiro", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("VPN", fontSize = 18.sp, color = Color(0xFFE94560), letterSpacing = 8.sp)

            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) Color(0xFF00D26A) else Color(0xFFE94560))
                    .clickable {
                        scope.launch {
                            if (!isConnected) {
                                val intent = Intent(context, AmiroVpnService::class.java)
                                intent.action = AmiroVpnService.ACTION_CONNECT
                                context.startService(intent)
                            } else {
                                val intent = Intent(context, AmiroVpnService::class.java)
                                intent.action = AmiroVpnService.ACTION_DISCONNECT
                                context.startService(intent)
                            }
                            isConnected = !isConnected
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isConnected) "قطع" else "اتصال",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("سرور", selectedServer)
                    InfoRow("تاخیر", "${latency}ms")
                    InfoRow("سرعت", "${"%.1f".format(speed)} Mbps")
                    InfoRow("پروتکل", "VMess")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        val scanner = ServerScanner()
                        val results = scanner.scanAllServers()
                        if (results.isNotEmpty()) {
                            val best = results.minByOrNull { it.latency }
                            best?.let {
                                selectedServer = "${it.address}:${it.port}"
                                latency = it.latency
                                speed = it.speed
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0f3460)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("اسکن هوشمند سرورها")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp)
    }
}
