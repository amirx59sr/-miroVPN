package com.amirovpn

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class AmiroApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_VPN,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AmiroVPN connection status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    companion object {
        lateinit var instance: AmiroApp
            private set
        const val CHANNEL_VPN = "vpn_service"
    }
}