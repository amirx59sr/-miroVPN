package com.amirovpn.subscription

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SubscriptionManager {
    
    private val TAG = "SubscriptionManager"
    
    private val client = HttpClient(Android) {
        engine {
            connectTimeout = 10_000
            socketTimeout = 10_000
        }
    }
    
    // لیست منابع کانفیگ (GitHub، تلگرام و...)
    private val configSources = listOf(
        // GitHub repositories
        "https://raw.githubusercontent.com/barry-far/V2ray-Configs/main/Sub_Merge.txt",
        "https://raw.githubusercontent.com/soroushmirzaei/telegram-configs-collector/main/protocols/reality",
        "https://raw.githubusercontent.com/soroushmirzaei/telegram-configs-collector/main/protocols/vmess",
        "https://raw.githubusercontent.com/soroushmirzaei/telegram-configs-collector/main/protocols/vless",
        "https://raw.githubusercontent.com/soroushmirzaei/telegram-configs-collector/main/protocols/trojan",
        "https://raw.githubusercontent.com/yebekhe/TelegramV2rayCollector/main/sub/mix/mix_base64",
        
        // تلگرام کانال‌ها
        "https://raw.githubusercontent.com/IranianCypherpunks/Sub/main/config",
        "https://raw.githubusercontent.com/MrPooyaX/VpnsFucking/main/Shenzo.txt"
    )
    
    /**
     * دانلود همه کانفیگ‌ها از منابع مختلف
     */
    suspend fun fetchAllConfigs(): List<String> {
        return withContext(Dispatchers.IO) {
            val allConfigs = mutableListOf<String>()
            
            for (url in configSources) {
                try {
                    Log.d(TAG, "Fetching from: $url")
                    val response = client.get(url)
                    val content = response.bodyAsText()
                    
                    // پارس کردن کانفیگ‌ها
                    val configs = parseConfigs(content)
                    allConfigs.addAll(configs)
                    
                    Log.d(TAG, "Found ${configs.size} configs from $url")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching from $url", e)
                }
            }
            
            Log.d(TAG, "Total configs fetched: ${allConfigs.size}")
            return@withContext allConfigs
        }
    }
    
    /**
     * پارس کردن متن کانفیگ
     */
    private fun parseConfigs(content: String): List<String> {
        val configs = mutableListOf<String>()
        
        // اگر Base64 encoded باشه
        try {
            val decoded = String(android.util.Base64.decode(content, android.util.Base64.DEFAULT))
            val lines = decoded.lines()
            configs.addAll(lines.filter { it.startsWith("vmess://") || it.startsWith("vless://") || 
                                          it.startsWith("trojan://") || it.startsWith("ss://") ||
                                          it.startsWith("tuic://") || it.startsWith("hysteria2://") })
        } catch (e: Exception) {
            // اگر Base64 نباشه، مستقیم پارس کن
            val lines = content.lines()
            configs.addAll(lines.filter { it.startsWith("vmess://") || it.startsWith("vless://") || 
                                          it.startsWith("trojan://") || it.startsWith("ss://") ||
                                          it.startsWith("tuic://") || it.startsWith("hysteria2://") })
        }
        
        return configs
    }
    
    /**
     * دانلود یک ساب‌لینک خاص
     */
    suspend fun fetchSubscription(url: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching subscription: $url")
                val response = client.get(url)
                val content = response.bodyAsText()
                return@withContext parseConfigs(content)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching subscription", e)
                return@withContext emptyList()
            }
        }
    }
    
    fun close() {
        client.close()
    }
}