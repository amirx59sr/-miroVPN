package com.amirovpn.scanner

import android.util.Log
import com.amirovpn.core.ConfigParser
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class ServerScanner {
    
    private val TAG = "ServerScanner"
    private val configParser = ConfigParser()
    
    // تنظیمات بهینه برای گوشی (فشار کم)
    private val MAX_CONCURRENT_SCANS = 3  // حداکثر ۳ سرور همزمان (بهینه برای CPU)
    private val PING_TIMEOUT = 3000       // ۳ ثانیه برای پینگ
    private val SITE_TIMEOUT = 5000       // ۵ ثانیه برای تست سایت
    private val DELAY_BETWEEN_TESTS = 100 // ۱۰۰ میلی‌ثانیه بین تست‌ها (جلوگیری از داغ شدن)
    
    // سرورهای تست برای تلگرام و اینستاگرام
    private val testTargets = listOf(
        TestTarget("Telegram", "api.telegram.org", 443, weight = 0.5),
        TestTarget("Instagram", "i.instagram.com", 443, weight = 0.5)
    )
    
    data class TestTarget(
        val name: String,
        val host: String,
        val port: Int,
        val weight: Double  // اهمیت این تست (۰ تا ۱)
    )
    
    data class ScanResult(
        val configUri: String,
        val serverName: String,
        val pingMs: Long,
        val telegramAccessible: Boolean,
        val instagramAccessible: Boolean,
        val score: Double,  // امتیاز نهایی (۰ تا ۱۰۰)
        val protocol: String
    )
    
    /**
     * اسکن همه کانفیگ‌ها و پیدا کردن بهترین سرور
     */
    suspend fun scanAll(configs: List<String>, onProgress: (Int, Int) -> Unit = { _, _ -> }): List<ScanResult> {
        Log.d(TAG, "Starting scan of ${configs.size} configs")
        
        val semaphore = Semaphore(MAX_CONCURRENT_SCANS)
        val results = mutableListOf<ScanResult>()
        var completed = 0
        
        // استفاده از coroutine با محدودیت برای جلوگیری از فشار به CPU
        coroutineScope {
            val jobs = configs.map { config ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        try {
                            val result = scanSingleConfig(config)
                            if (result != null) {
                                synchronized(results) {
                                    results.add(result)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error scanning config", e)
                        } finally {
                            completed++
                            onProgress(completed, configs.size)
                            delay(DELAY_BETWEEN_TESTS) // جلوگیری از داغ شدن
                        }
                    }
                }
            }
            
            jobs.awaitAll()
        }
        
        // مرتب‌سازی بر اساس امتیاز (بالاترین اول)
        return results.sortedByDescending { it.score }
    }
    
    /**
     * اسکن یک کانفیگ منفرد
     */
    private suspend fun scanSingleConfig(configUri: String): ScanResult? {
        return try {
            // استخراج اطلاعات سرور
            val serverInfo = extractServerInfo(configUri) ?: return null
            
            // مرحله ۱: تست پینگ (سریع و سبک)
            val pingMs = pingServer(serverInfo.host, serverInfo.port)
            if (pingMs < 0) return null // سرور پاسخ نداد
            
            // مرحله ۲: تست تلگرام و اینستاگرام
            val telegramOk = testSiteAccess("Telegram", "api.telegram.org")
            val instagramOk = testSiteAccess("Instagram", "i.instagram.com")
            
            // اگر هیچکدوم وصل نشدن، این سرور به درد نمی‌خوره
            if (!telegramOk && !instagramOk) return null
            
            // محاسبه امتیاز
            val score = calculateScore(pingMs, telegramOk, instagramOk)
            
            val serverName = extractServerName(configUri)
            
            ScanResult(
                configUri = configUri,
                serverName = serverName,
                pingMs = pingMs,
                telegramAccessible = telegramOk,
                instagramAccessible = instagramOk,
                score = score,
                protocol = serverInfo.protocol
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in scanSingleConfig", e)
            null
        }
    }
    
    /**
     * تست پینگ TCP (سریع و سبک)
     */
    private suspend fun pingServer(host: String, port: Int): Long {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val socket = Socket()
                socket.soTimeout = PING_TIMEOUT
                socket.connect(InetSocketAddress(host, port), PING_TIMEOUT)
                val pingMs = System.currentTimeMillis() - startTime
                socket.close()
                pingMs
            } catch (e: Exception) {
                -1
            }
        }
    }
    
    /**
     * تست دسترسی به یه سایت (تلگرام یا اینستاگرام)
     */
    private suspend fun testSiteAccess(name: String, host: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://$host")
                val connection = url.openConnection() as HttpsURLConnection
                connection.connectTimeout = SITE_TIMEOUT
                connection.readTimeout = SITE_TIMEOUT
                connection.requestMethod = "HEAD"
                connection.instanceFollowRedirects = false
                
                val responseCode = connection.responseCode
                connection.disconnect()
                
                // اگر کد پاسخ ۲۰۰، ۳۰۱، ۳۰۲ یا ۴۰۴ باشه، یعنی سرور در دسترسه
                // (۴۰۴ هم یعنی اتصال برقرار شده)
                responseCode in listOf(200, 301, 302, 404, 405)
                
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * محاسبه امتیاز هوشمند
     */
    private fun calculateScore(pingMs: Long, telegramOk: Boolean, instagramOk: Boolean): Double {
        var score = 0.0
        
        // امتیاز پینگ (حداکثر ۴۰ امتیاز)
        // هرچه پینگ کمتر، امتیاز بیشتر
        val pingScore = when {
            pingMs < 100 -> 40.0
            pingMs < 200 -> 30.0
            pingMs < 300 -> 20.0
            pingMs < 500 -> 10.0
            else -> 5.0
        }
        score += pingScore
        
        // امتیاز تلگرام (۳۰ امتیاز)
        if (telegramOk) score += 30.0
        
        // امتیاز اینستاگرام (۳۰ امتیاز)
        if (instagramOk) score += 30.0
        
        return score
    }
    
    /**
     * استخراج اطلاعات سرور از URI
     */
    private fun extractServerInfo(uri: String): ServerInfo? {
        return try {
            when {
                uri.startsWith("vless://") -> {
                    val withoutPrefix = uri.removePrefix("vless://")
                    val parts = withoutPrefix.split("@")
                    val serverPart = parts[1].split("?")[0]
                    val server = serverPart.split(":")[0]
                    val port = serverPart.split(":")[1].toInt()
                    ServerInfo(server, port, "VLESS")
                }
                uri.startsWith("vmess://") -> {
                    val base64Part = uri.removePrefix("vmess://")
                    val decoded = String(android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT))
                    val json = org.json.JSONObject(decoded)
                    ServerInfo(json.getString("add"), json.getString("port").toInt(), "VMESS")
                }
                uri.startsWith("trojan://") -> {
                    val withoutPrefix = uri.removePrefix("trojan://")
                    val parts = withoutPrefix.split("@")
                    val serverPart = parts[1].split("?")[0]
                    val server = serverPart.split(":")[0]
                    val port = serverPart.split(":")[1].toInt()
                    ServerInfo(server, port, "TROJAN")
                }
                uri.startsWith("ss://") -> {
                    val withoutPrefix = uri.removePrefix("ss://")
                    val parts = withoutPrefix.split("@")
                    val serverPart = parts[1].split("#")[0]
                    val server = serverPart.split(":")[0]
                    val port = serverPart.split(":")[1].toInt()
                    ServerInfo(server, port, "SHADOWSOCKS")
                }
                uri.startsWith("tuic://") -> {
                    val withoutPrefix = uri.removePrefix("tuic://")
                    val parts = withoutPrefix.split("@")
                    val serverPart = parts[1].split("?")[0]
                    val server = serverPart.split(":")[0]
                    val port = serverPart.split(":")[1].toInt()
                    ServerInfo(server, port, "TUIC")
                }
                uri.startsWith("hysteria2://") -> {
                    val withoutPrefix = uri.removePrefix("hysteria2://")
                    val parts = withoutPrefix.split("@")
                    val serverPart = parts[1].split("?")[0]
                    val server = serverPart.split(":")[0]
                    val port = serverPart.split(":")[1].toInt()
                    ServerInfo(server, port, "HYSTERIA2")
                }
                else -> null
            }
        } catch (e: Exception) {
            null
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
    
    data class ServerInfo(
        val host: String,
        val port: Int,
        val protocol: String
    )
}