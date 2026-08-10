package com.amirovpn.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amirovpn.core.ConfigParser
import com.amirovpn.vpn.AmiroVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VpnViewModel(application: Application) : AndroidViewModel(application) {
    
    private val configParser = ConfigParser()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    private val _currentServer = MutableStateFlow("Auto-Select 🤖")
    val currentServer: StateFlow<String> = _currentServer
    
    private val _ping = MutableStateFlow("--")
    val ping: StateFlow<String> = _ping
    
    private val _speed = MutableStateFlow("--")
    val speed: StateFlow<String> = _speed
    
    /**
     * اتصال با کانفیگ واقعی
     */
    fun connectWithConfig(configUri: String) {
        viewModelScope.launch {
            try {
                // تبدیل URI به فرمت sing-box JSON
                val configJson = configParser.parseConfig(configUri)
                
                if (configJson == null) {
                    _currentServer.value = "Invalid Config"
                    return@launch
                }
                
                // شروع سرویس VPN
                val intent = Intent(getApplication(), AmiroVpnService::class.java).apply {
                    action = "START"
                    putExtra("CONFIG_JSON", configJson)
                }
                
                getApplication<Application>().startForegroundService(intent)
                
                _isConnected.value = true
                _currentServer.value = extractServerName(configUri)
                _ping.value = "Active"
                _speed.value = "Monitoring..."
                
            } catch (e: Exception) {
                _isConnected.value = false
                _currentServer.value = "Connection Failed"
            }
        }
    }
    
    /**
     * اتصال با کانفیگ پیش‌فرض (Direct)
     */
    fun connect() {
        val defaultConfig = """
        {
            "log": {
                "level": "info"
            },
            "dns": {
                "servers": [
                    {
                        "tag": "google",
                        "address": "tls://8.8.8.8"
                    }
                ]
            },
            "inbounds": [
                {
                    "type": "tun",
                    "tag": "tun-in",
                    "inet4_address": "172.19.0.1/30",
                    "auto_route": true,
                    "strict_route": true,
                    "stack": "system",
                    "sniff": true
                }
            ],
            "outbounds": [
                {
                    "type": "direct",
                    "tag": "direct"
                }
            ]
        }
        """.trimIndent()
        
        viewModelScope.launch {
            try {
                val intent = Intent(getApplication(), AmiroVpnService::class.java).apply {
                    action = "START"
                    putExtra("CONFIG_JSON", defaultConfig)
                }
                
                getApplication<Application>().startForegroundService(intent)
                
                _isConnected.value = true
                _currentServer.value = "Direct Connection"
                _ping.value = "Active"
                _speed.value = "Monitoring..."
                
            } catch (e: Exception) {
                _isConnected.value = false
            }
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            try {
                val intent = Intent(getApplication(), AmiroVpnService::class.java).apply {
                    action = "STOP"
                }
                getApplication<Application>().startService(intent)
                
                _isConnected.value = false
                _currentServer.value = "Auto-Select 🤖"
                _ping.value = "--"
                _speed.value = "--"
                
            } catch (e: Exception) {
                // خطا
            }
        }
    }
    
    /**
     * استخراج نام سرور از URI
     */
    private fun extractServerName(uri: String): String {
        return try {
            if (uri.contains("#")) {
                java.net.URLDecoder.decode(uri.substringAfter("#"), "UTF-8")
            } else {
                "Unknown Server"
            }
        } catch (e: Exception) {
            "Unknown Server"
        }
    }
}