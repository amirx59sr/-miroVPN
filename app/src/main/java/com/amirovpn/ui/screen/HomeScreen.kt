package com.amirovpn.ui.screen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.amirovpn.scanner.ServerScanner
import com.amirovpn.ui.viewmodel.ScanViewModel
import com.amirovpn.ui.viewmodel.VpnViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vpnViewModel: VpnViewModel = viewModel(),
    scanViewModel: ScanViewModel = viewModel()
) {
    val context = LocalContext.current
    
    val isConnected by vpnViewModel.isConnected.collectAsState()
    val currentServer by vpnViewModel.currentServer.collectAsState()
    val ping by vpnViewModel.ping.collectAsState()
    val speed by vpnViewModel.speed.collectAsState()
    
    val isScanning by scanViewModel.isScanning.collectAsState()
    val scanProgress by scanViewModel.scanProgress.collectAsState()
    val scanTotal by scanViewModel.scanTotal.collectAsState()
    val scanResults by scanViewModel.scanResults.collectAsState()
    val bestServer by scanViewModel.bestServer.collectAsState()
    
    // لانچر درخواست مجوز VPN (با ایمپورت صحیح)
    val vpnRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            bestServer?.let { server ->
                vpnViewModel.connectWithConfig(server.configUri)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Âmiro VPN", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier.size(200.dp).clip(CircleShape)
                        .background(if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            if (isConnected) {
                                vpnViewModel.disconnect()
                            } else {
                                if (bestServer != null) {
                                    val intent = android.net.VpnService.prepare(context)
                                    if (intent != null) {
                                        vpnRequestLauncher.launch(intent)
                                    } else {
                                        vpnViewModel.connectWithConfig(bestServer!!.configUri)
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isConnected) "Connected" else "Tap to Connect",
                            color = if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        StatusRow("Server", currentServer)
                        StatusRow("Ping", ping)
                        StatusRow("Speed", speed)
                    }
                }
            }
            
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    OutlinedButton(onClick = { /* Subs */ }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Link, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Subs")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { scanViewModel.startScan() }, enabled = !isScanning, modifier = Modifier.weight(1f)) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Speed, null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (isScanning) "Scanning..." else "Scan")
                    }
                }
            }
            
            if (isScanning) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Scanning servers...", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = if (scanTotal > 0) scanProgress.toFloat() / scanTotal else 0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("$scanProgress / $scanTotal servers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            
            if (bestServer != null && !isScanning) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🏆 Best Server", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Score: ${bestServer!!.score.toInt()}/100", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(bestServer!!.serverName, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ping: ${bestServer!!.pingMs}ms")
                                Text(bestServer!!.protocol)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Text(if (bestServer!!.telegramAccessible) "✅ Telegram" else "❌ Telegram", fontSize = 12.sp)
                                Text(if (bestServer!!.instagramAccessible) "✅ Instagram" else "❌ Instagram", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            
            if (scanResults.isNotEmpty() && !isScanning) {
                item {
                    Text("All Servers (${scanResults.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                }
                items(scanResults.take(10)) { result ->
                    ServerCard(result)
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ServerCard(result: ServerScanner.ScanResult) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(result.serverName, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("${result.score.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${result.pingMs}ms", fontSize = 12.sp)
                Text(result.protocol, fontSize = 12.sp)
                Text(if (result.telegramAccessible && result.instagramAccessible) "✅✅" else if (result.telegramAccessible) "✅❌" else "❌✅", fontSize = 12.sp)
            }
        }
    }
}