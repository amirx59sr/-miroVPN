package com.amiro.vpn.core

import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.system.measureTimeMillis

class ServerScanner {

    data class ServerResult(
        val address: String,
        val port: Int,
        val latency: Long,
        val protocol: String,
        val speed: Double
    )

    suspend fun scanAllServers(): List<ServerResult> = coroutineScope {
        val configs = fetchGithubConfigs()
        configs.map { config ->
            async(Dispatchers.IO) { scanServer(config) }
        }.awaitAll().filterNotNull()
    }

    private suspend fun fetchGithubConfigs(): List<String> = withContext(Dispatchers.IO) {
        val configs = mutableListOf<String>()
        val urls = listOf(
            "https://raw.githubusercontent.com/v2ray/v2ray-core/master/release/config.json",
            "https://raw.githubusercontent.com/FreeV2ray/config/main/config.json"
        )
        urls.forEach { url ->
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                val content = connection.inputStream.bufferedReader().readText()
                extractConfigs(content).forEach { configs.add(it) }
            } catch (_: Exception) {}
        }
        configs
    }

    private fun scanServer(config: String): ServerResult? {
        return try {
            val parsed = parseConfig(config) ?: return null
            val latency = measureTimeMillis {
                Socket().use { it.connect(InetSocketAddress(parsed.first, parsed.second), 2000) }
            }
            ServerResult(
                address = parsed.first,
                port = parsed.second,
                latency = latency,
                protocol = parsed.third,
                speed = 10.0
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseConfig(config: String): Triple<String, Int, String>? {
        return when {
            config.startsWith("vmess://") -> {
                val json = String(android.util.Base64.decode(
                    config.removePrefix("vmess://"), android.util.Base64.DEFAULT
                ))
                val obj = org.json.JSONObject(json)
                Triple(obj.getString("add"), obj.getInt("port"), "VMess")
            }
            config.startsWith("vless://") -> {
                val uri = java.net.URI(config)
                Triple(uri.host ?: "", uri.port, "VLESS")
            }
            else -> null
        }
    }

    private fun extractConfigs(content: String): List<String> {
        val configs = mutableListOf<String>()
        Regex("vmess://[A-Za-z0-9+/=]+").findAll(content).forEach { configs.add(it.value) }
        Regex("vless://[A-Za-z0-9@:.?&=#]+").findAll(content).forEach { configs.add(it.value) }
        return configs
    }
}
