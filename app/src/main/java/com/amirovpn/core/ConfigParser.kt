package com.amirovpn.core

import android.util.Base64
import android.util.Log
import org.json.JSONObject

class ConfigParser {
    
    private val TAG = "ConfigParser"
    
    /**
     * تبدیل URI کانفیگ به فرمت JSON sing-box
     */
    fun parseConfig(uri: String): String? {
        return try {
            when {
                uri.startsWith("vless://") -> parseVLESS(uri)
                uri.startsWith("vmess://") -> parseVMESS(uri)
                uri.startsWith("trojan://") -> parseTrojan(uri)
                uri.startsWith("ss://") -> parseShadowsocks(uri)
                uri.startsWith("tuic://") -> parseTUIC(uri)
                uri.startsWith("hysteria2://") -> parseHysteria2(uri)
                else -> {
                    Log.w(TAG, "Unsupported protocol: $uri")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing config", e)
            null
        }
    }
    
    /**
     * پارس VLESS
     */
    private fun parseVLESS(uri: String): String {
        // vless://uuid@server:port?params#name
        val withoutPrefix = uri.removePrefix("vless://")
        val parts = withoutPrefix.split("@")
        val uuid = parts[0]
        val serverPart = parts[1].split("?")[0]
        val server = serverPart.split(":")[0]
        val port = serverPart.split(":")[1].toInt()
        
        val params = uri.substringAfter("?").substringBefore("#")
        val queryParams = parseQueryParams(params)
        
        val security = queryParams["security"] ?: "none"
        val flow = queryParams["flow"] ?: ""
        val sni = queryParams["sni"] ?: ""
        val fp = queryParams["fp"] ?: "chrome"
        
        val outbound = JSONObject().apply {
            put("type", "vless")
            put("tag", "proxy")
            put("server", server)
            put("server_port", port)
            put("uuid", uuid)
            
            if (flow.isNotEmpty()) {
                put("flow", flow)
            }
            
            put("tls", JSONObject().apply {
                put("enabled", security != "none")
                put("server_name", sni)
                if (security == "reality") {
                    put("reality", JSONObject().apply {
                        put("enabled", true)
                        put("public_key", queryParams["pbk"] ?: "")
                        put("short_id", queryParams["sid"] ?: "")
                    })
                    put("utls", JSONObject().apply {
                        put("enabled", true)
                        put("fingerprint", fp)
                    })
                }
            })
            
            put("transport", getTransportConfig(queryParams))
        }
        
        return buildSingboxConfig(outbound)
    }
    
    /**
     * پارس VMESS
     */
    private fun parseVMESS(uri: String): String {
        // vmess://base64encoded
        val base64Part = uri.removePrefix("vmess://")
        val decoded = String(Base64.decode(base64Part, Base64.DEFAULT))
        val json = JSONObject(decoded)
        
        val outbound = JSONObject().apply {
            put("type", "vmess")
            put("tag", "proxy")
            put("server", json.getString("add"))
            put("server_port", json.getString("port").toInt())
            put("uuid", json.getString("id"))
            put("alter_id", json.optString("aid", "0").toInt())
            put("security", json.optString("scy", "auto"))
            
            if (json.optString("tls") == "tls") {
                put("tls", JSONObject().apply {
                    put("enabled", true)
                    put("server_name", json.optString("sni", ""))
                })
            }
            
            put("transport", JSONObject().apply {
                put("type", json.optString("net", "tcp"))
                if (json.optString("net") == "ws") {
                    put("path", json.optString("path", "/"))
                    put("headers", JSONObject().apply {
                        put("Host", json.optString("host", ""))
                    })
                }
            })
        }
        
        return buildSingboxConfig(outbound)
    }
    
    /**
     * پارس Trojan
     */
    private fun parseTrojan(uri: String): String {
        // trojan://password@server:port?params#name
        val withoutPrefix = uri.removePrefix("trojan://")
        val parts = withoutPrefix.split("@")
        val password = parts[0]
        val serverPart = parts[1].split("?")[0]
        val server = serverPart.split(":")[0]
        val port = serverPart.split(":")[1].toInt()
        
        val params = uri.substringAfter("?").substringBefore("#")
        val queryParams = parseQueryParams(params)
        
        val outbound = JSONObject().apply {
            put("type", "trojan")
            put("tag", "proxy")
            put("server", server)
            put("server_port", port)
            put("password", password)
            
            put("tls", JSONObject().apply {
                put("enabled", true)
                put("server_name", queryParams["sni"] ?: server)
            })
        }
        
        return buildSingboxConfig(outbound)
    }
    
    /**
     * پارس Shadowsocks
     */
    private fun parseShadowsocks(uri: String): String {
        // ss://method:password@server:port#name
        val withoutPrefix = uri.removePrefix("ss://")
        val parts = withoutPrefix.split("@")
        val methodPassword = String(Base64.decode(parts[0], Base64.DEFAULT))
        val method = methodPassword.split(":")[0]
        val password = methodPassword.split(":")[1]
        val serverPart = parts[1].split("#")[0]
        val server = serverPart.split(":")[0]
        val port = serverPart.split(":")[1].toInt()
        
        val outbound = JSONObject().apply {
            put("type", "shadowsocks")
            put("tag", "proxy")
            put("server", server)
            put("server_port", port)
            put("method", method)
            put("password", password)
        }
        
        return buildSingboxConfig(outbound)
    }
    
    /**
     * پارس TUIC
     */
    private fun parseTUIC(uri: String): String {
        // tuic://uuid:password@server:port?params#name
        val withoutPrefix = uri.removePrefix("tuic://")
        val parts = withoutPrefix.split("@")
        val uuidPassword = parts[0]
        val uuid = uuidPassword.split(":")[0]
        val password = uuidPassword.split(":")[1]
        val serverPart = parts[1].split("?")[0]
        val server = serverPart.split(":")[0]
        val port = serverPart.split(":")[1].toInt()
        
        val outbound = JSONObject().apply {
            put("type", "tuic")
            put("tag", "proxy")
            put("server", server)
            put("server_port", port)
            put("uuid", uuid)
            put("password", password)
        }
        
        return buildSingboxConfig(outbound)
    }
    
    /**
     * پارس Hysteria2
     */
    private fun parseHysteria2(uri: String): String {
        // hysteria2://password@server:port?params#name
        val withoutPrefix = uri.removePrefix("hysteria2://")
        val parts = withoutPrefix.split("@")
        val password = parts[0]
        val serverPart = parts[1].split("?")[0]
        val server = serverPart.split(":")[0]
        val port = serverPart.split(":")[1].toInt()
        
        val outbound = JSONObject().apply {
            put("type", "hysteria2")
            put("tag", "proxy")
            put("server", server)
            put("server_port", port)
            put("password", password)
        }
        
        return buildSingboxConfig(outbound)
    }
    
    /**
     * ساخت کانفیگ کامل sing-box
     */
    private fun buildSingboxConfig(outbound: JSONObject): String {
        val config = JSONObject().apply {
            put("log", JSONObject().apply {
                put("level", "info")
            })
            
            put("dns", JSONObject().apply {
                put("servers", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("tag", "google")
                        put("address", "tls://8.8.8.8")
                    })
                })
            })
            
            put("inbounds", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "tun")
                    put("tag", "tun-in")
                    put("inet4_address", "172.19.0.1/30")
                    put("auto_route", true)
                    put("strict_route", true)
                    put("stack", "system")
                    put("sniff", true)
                })
            })
            
            put("outbounds", org.json.JSONArray().apply {
                put(outbound)
                put(JSONObject().apply {
                    put("type", "direct")
                    put("tag", "direct")
                })
            })
        }
        
        return config.toString(2)
    }
    
    /**
     * پارس Query Parameters
     */
    private fun parseQueryParams(query: String): Map<String, String> {
        return query.split("&").mapNotNull { param ->
            val parts = param.split("=")
            if (parts.size == 2) {
                parts[0] to java.net.URLDecoder.decode(parts[1], "UTF-8")
            } else null
        }.toMap()
    }
    
    /**
     * دریافت تنظیمات Transport
     */
    private fun getTransportConfig(params: Map<String, String>): JSONObject {
        val type = params["type"] ?: "tcp"
        
        return JSONObject().apply {
            put("type", type)
            
            when (type) {
                "ws" -> {
                    put("path", params["path"] ?: "/")
                    if (params.containsKey("host")) {
                        put("headers", JSONObject().apply {
                            put("Host", params["host"])
                        })
                    }
                }
                "grpc" -> {
                    put("service_name", params["serviceName"] ?: "")
                }
            }
        }
    }
}