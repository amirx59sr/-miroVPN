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

class AmiroVpnService : VpnService() {

    private val TAG = "AmiroVpnService"
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VPN Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "VPN Service onStartCommand")
        
        val action = intent?.action
        
        if (action == "START") {
            startVpn()
        } else if (action == "STOP") {
            stopVpn()
        }
        
        return START_STICKY
    }

    private fun startVpn() {
        Log.d(TAG, "Starting VPN...")
        
        // نمایش نوتیفیکیشن دائمی
        startForeground(1, createNotification())
        
        try {
            // تنظیمات پایه‌ی تونل VPN
            val builder = Builder()
                .setSession("Âmiro VPN")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0) // همه ترافیک رو هدایت کن
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
            
            vpnInterface = builder.establish()
            Log.d(TAG, "VPN Interface Established Successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error establishing VPN", e)
        }
    }

    private fun stopVpn() {
        Log.d(TAG, "Stopping VPN...")
        
        vpnInterface?.close()
        vpnInterface = null
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "VPN Service Destroyed")
        stopVpn()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, 0)
        }

        return NotificationCompat.Builder(this, AmiroApp.CHANNEL_VPN)
            .setContentTitle("Âmiro VPN")
            .setContentText("Connected and protecting your traffic 🛡️")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // آیکون پیش‌فرض اندروید
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}