package com.amirovpn.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CoreManager(private val context: Context) {
    
    private val TAG = "CoreManager"
    private var singboxProcess: Process? = null
    private var isRunning = false
    
    companion object {
        init {
            System.loadLibrary("tun2socks")
        }
    }
    
    /**
     * کپی فایل sing-box از assets به حافظه داخلی
     */
    private suspend fun copySingboxBinary(): File {
        return withContext(Dispatchers.IO) {
            val abi = System.getProperty("os.arch")
            val binaryName = when {
                abi?.contains("aarch64") == true -> "sing-box-arm64"
                abi?.contains("arm") == true -> "sing-box-armv7"
                abi?.contains("x86_64") == true -> "sing-box-x86_64"
                else -> "sing-box-arm64"
            }
            
            val outputFile = File(context.filesDir, "sing-box")
            
            if (!outputFile.exists()) {
                context.assets.open(binaryName).use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                outputFile.setExecutable(true)
                Log.d(TAG, "sing-box binary copied to: ${outputFile.absolutePath}")
            }
            
            return@withContext outputFile
        }
    }
    
    /**
     * شروع هسته sing-box با کانفیگ JSON
     */
    suspend fun start(configJson: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isRunning) {
                    Log.w(TAG, "Core already running")
                    return@withContext true
                }
                
                Log.d(TAG, "Starting sing-box core...")
                
                // کپی فایل اجرایی
                val binary = copySingboxBinary()
                
                // ذخیره کانفیگ در فایل موقت
                val configFile = File(context.filesDir, "config.json")
                configFile.writeText(configJson)
                
                // اجرای sing-box
                val processBuilder = ProcessBuilder(
                    binary.absolutePath,
                    "run",
                    "-c", configFile.absolutePath
                )
                processBuilder.redirectErrorStream(true)
                
                singboxProcess = processBuilder.start()
                isRunning = true
                
                Log.d(TAG, "sing-box started successfully")
                
                // خواندن لاگ‌ها (برای دیباگ)
                Thread {
                    singboxProcess?.inputStream?.bufferedReader()?.use { reader ->
                        reader.forEachLine { line ->
                            Log.d("sing-box", line)
                        }
                    }
                }.start()
                
                return@withContext true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting sing-box", e)
                return@withContext false
            }
        }
    }
    
    /**
     * توقف هسته
     */
    suspend fun stop() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Stopping sing-box core...")
                singboxProcess?.destroy()
                singboxProcess?.waitFor()
                singboxProcess = null
                isRunning = false
                Log.d(TAG, "sing-box stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping sing-box", e)
            }
        }
    }
    
    /**
     * بررسی وضعیت اجرا
     */
    fun isRunning(): Boolean {
        return isRunning && singboxProcess?.isAlive == true
    }
    
    /**
     * دریافت نسخه هسته
     */
    fun getVersion(): String {
        return "sing-box 1.8.0"
    }
}