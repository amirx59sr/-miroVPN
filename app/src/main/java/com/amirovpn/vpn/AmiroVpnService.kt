package com.amirovpn.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.amirovpn.AmiroApp
import com.amirovpn.MainActivity
import com.amirovpn.core.CoreManager
import kotlinx.coroutines.*

class AmiroVpnService : VpnService() {

    private val TAG = "AmiroVpnService"
    private var vpnInterface: ParcelFileDescriptor? = null
    private var coreManager: CoreManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VPN Service Created")
        coreManager = CoreManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "VPN Service onStartCommand")
        
        val action = intent?.action
        val configJson = intent?.getStringExtra("CONFIG_JSON")
        
        when (action) {
            "START" -> {
                if (configJson != null) {
                    startVpn(configJson)
                } else {
                    Log.e(TAG, "No config provided")
                    stopSelf()
                }
            }
            "STOP" -> stopVpn()
        }
        
        return START_STICKY
    }

    private fun startVpn(configJson: String) {
        Log.d(TAG, "Starting VPN...")
        
        // نمایش نوتیفیکیشن دائمی
        startForeground(1, createNotification("Connecting..."))
        
        serviceScope.launch {
            try {
                // تنظیمات پایه‌ی تونل VPN
                val builder = Builder()
                    .setSession("Âmiro VPN")
                    .addAddress("10.0.0.2", 32)
                    .addRoute("0.0.0.0", 0) // همه ترافیک رو هدایت کن
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")
                    .setMtu(1500)
                
                vpnInterface = builder.establish()
                
                if (vpnInterface == null) {
                    Log.e(TAG, "Failed to establish VPN interface")
                    stopSelf()
                    return@launch
                }
                
                Log.d(TAG, "VPN Interface Established")
                
                // شروع هسته sing-box
                val success = coreManager?.start(configJson) ?: false
                
                if (success) {
                    Log.d(TAG, "sing-box started successfully")
                    updateNotification("Connected 🛡️")
                } else {
                    Log.e(TAG, "Failed to start sing-box")
                    stopSelf()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting VPN", e)
                stopSelf()
            }
        }
    }

    private fun stopVpn() {
        Log.d(TAG, "Stopping VPN...")
        
        serviceScope.launch {
            try {
                coreManager?.stop()
                vpnInterface?.close()
                vpnInterface = null
                
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                
                Log.d(TAG, "VPN stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping VPN", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "VPN Service Destroyed")
        serviceScope.cancel()
        stopVpn()
    }

    override fun onRevoke() {
        super.onRevoke()
        Log.d(TAG, "VPN Revoked")
        stopVpn()
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, 0)
        }

        return NotificationCompat.Builder(this, AmiroApp.CHANNEL_VPN)
            .setContentTitle("Âmiro VPN")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(1, notification)
    }
}