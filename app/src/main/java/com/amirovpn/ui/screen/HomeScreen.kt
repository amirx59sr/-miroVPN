package com.amirovpn.ui.screen

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amirovpn.ui.viewmodel.VpnViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: VpnViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // دریافت State از ViewModel
    val isConnected by viewModel.isConnected.collectAsState()
    val currentServer by viewModel.currentServer.collectAsState()
    val ping by viewModel.ping.collectAsState()
    val speed by viewModel.speed.collectAsState()
    
    // درخواست مجوز VPN
    val vpnRequestLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.connect(Intent(context, com.amirovpn.vpn.AmiroVpnService::class.java))
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Âmiro VPN",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // دکمه بزرگ اتصال
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) 
                            MaterialTheme.colorScheme.primary
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable {
                        if (isConnected) {
                            viewModel.disconnect()
                        } else {
                            // درخواست مجوز VPN
                            val intent = android.net.VpnService.prepare(context)
                            if (intent != null) {
                                vpnRequestLauncher.launch(intent)
                            } else {
                                viewModel.connect(Intent(context, com.amirovpn.vpn.AmiroVpnService::class.java))
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isConnected) 
                            Icons.Default.CheckCircle 
                        else 
                            Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (isConnected) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isConnected) "Connected" else "Tap to Connect",
                        color = if (isConnected) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // وضعیت اتصال
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    StatusRow("Server", currentServer)
                    StatusRow("Ping", ping)
                    StatusRow("Speed", speed)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // دکمه‌های پایین
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = { /* Subscriptions */ }) {
                    Icon(Icons.Default.Link, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Subscriptions")
                }
                OutlinedButton(onClick = { /* Scanner */ }) {
                    Icon(Icons.Default.Speed, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan")
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}