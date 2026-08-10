package com.amirovpn.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amirovpn.scanner.ServerScanner
import com.amirovpn.subscription.SubscriptionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    
    private val scanner = ServerScanner()
    private val subscriptionManager = SubscriptionManager()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning
    
    private val _scanProgress = MutableStateFlow(0)
    val scanProgress: StateFlow<Int> = _scanProgress
    
    private val _scanTotal = MutableStateFlow(0)
    val scanTotal: StateFlow<Int> = _scanTotal
    
    private val _scanResults = MutableStateFlow<List<ServerScanner.ScanResult>>(emptyList())
    val scanResults: StateFlow<List<ServerScanner.ScanResult>> = _scanResults
    
    private val _bestServer = MutableStateFlow<ServerScanner.ScanResult?>(null)
    val bestServer: StateFlow<ServerScanner.ScanResult?> = _bestServer
    
    /**
     * شروع اسکن همه سرورها
     */
    fun startScan() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanResults.value = emptyList()
            _bestServer.value = null
            
            try {
                // دانلود کانفیگ‌ها از منابع مختلف
                val configs = subscriptionManager.fetchAllConfigs()
                _scanTotal.value = configs.size
                
                // اسکن سرورها
                val results = scanner.scanAll(configs) { completed, total ->
                    _scanProgress.value = completed
                    _scanTotal.value = total
                }
                
                _scanResults.value = results
                
                // بهترین سرور
                if (results.isNotEmpty()) {
                    _bestServer.value = results.first()
                }
                
            } catch (e: Exception) {
                // خطا
            } finally {
                _isScanning.value = false
            }
        }
    }
    
    /**
     * دریافت بهترین سرور
     */
    fun getBestServer(): ServerScanner.ScanResult? {
        return _bestServer.value
    }
    
    override fun onCleared() {
        super.onCleared()
        subscriptionManager.close()
    }
}