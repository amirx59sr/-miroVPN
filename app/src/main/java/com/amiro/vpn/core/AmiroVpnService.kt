package com.amiro.vpn.core

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class AmiroVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val vpnScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    companion object {
        const val ACTION_CONNECT = "com.amiro.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.amiro.vpn.DISCONNECT"
        const val MTU = 1500
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val serverAddress = intent.getStringExtra("server_address") ?: "104.26.0.0"
                val serverPort = intent.getIntExtra("server_port", 443)
                startVpn(serverAddress, serverPort)
            }
            ACTION_DISCONNECT -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(serverAddress: String, serverPort: Int) {
        try {
            val builder = Builder()
                .setSession("Âmiro VPN")
                .addAddress("10.8.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .setMtu(MTU)
                .setBlocking(true)

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                isRunning = true
                showNotification("متصل شد")
                startPacketHandling(serverAddress, serverPort)
            }
        } catch (e: Exception) {
            Log.e("AmiroVPN", "خطا در اتصال", e)
            stopVpn()
        }
    }

    private fun startPacketHandling(server: String, port: Int) {
        vpnScope.launch {
            val input = FileInputStream(vpnInterface!!.fileDescriptor)
            val output = FileOutputStream(vpnInterface!!.fileDescriptor)
            val buffer = ByteArray(MTU)

            while (isRunning) {
                try {
                    val length = input.read(buffer)
                    if (length > 0) {
                        val packet = buffer.copyOf(length)
                        handlePacket(packet, output, server, port)
                    }
                } catch (e: Exception) {
                    if (isRunning) Log.e("AmiroVPN", "خطا", e)
                }
            }
        }
    }

    private suspend fun handlePacket(
        packet: ByteArray,
        output: FileOutputStream,
        server: String,
        port: Int
    ) = withContext(Dispatchers.IO) {
        try {
            val serverChannel = SocketChannel.open()
            serverChannel.connect(InetSocketAddress(server, port))
            serverChannel.write(ByteBuffer.wrap(packet))

            val responseBuffer = ByteBuffer.allocate(MTU)
            serverChannel.read(responseBuffer)

            responseBuffer.flip()
            val responseData = ByteArray(responseBuffer.remaining())
            responseBuffer.get(responseData)
            output.write(responseData)

            serverChannel.close()
        } catch (e: Exception) {
            Log.e("AmiroVPN", "خطای ارتباط", e)
        }
    }

    private fun stopVpn() {
        isRunning = false
        vpnInterface?.close()
        vpnInterface = null
        showNotification("قطع شد")
    }

    private fun showNotification(message: String) {
        val notification = Notification.Builder(this, "vpn_channel")
            .setContentTitle("Âmiro VPN")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
