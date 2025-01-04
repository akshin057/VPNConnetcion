package org.example.project.vpnconnection.Service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent
import android.health.connect.datatypes.units.Length
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import co.touchlab.stately.concurrency.AtomicBoolean
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

class MyVpnService : VpnService() {

    private val TAG: String = "MyVpnService"
    @Volatile
    private var isRunning: Boolean = false
    private lateinit var vpnInterface: ParcelFileDescriptor
    private lateinit var serverIp: String
    private lateinit var serverPortNumber: String
    private lateinit var domain: String
    private lateinit var uuid: String
    private lateinit var security: String
    private lateinit var path: String
    private var handler = Handler(Looper.getMainLooper())
    private val ACTION_VPN_CONNECTED = "org.example.project.vpnconnection.Service"
    private lateinit var instance: MyVpnService

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    fun getVpnInterface(): ParcelFileDescriptor {
        return vpnInterface
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {

            serverIp = intent.getStringExtra("vpnIp").toString()
            serverPortNumber = intent.getStringExtra("vpnPort").toString()
            uuid = intent.getStringExtra("uuid").toString()
            domain = intent.getStringExtra("domain").toString()
            path = intent.getStringExtra("path").toString()
            security = intent.getStringExtra("security").toString()

            GlobalScope.launch(Dispatchers.IO) {
                runVpnConnection()
            }

        }

        return START_STICKY
    }

    private fun runVpnConnection() {
        try {
            if (establishedVpnConnection()) {

                readFromVpnInterface()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Vpn connection: " + e.message)

        } finally {
            stopVpnConnection()
        }


    }

    private fun stopVpnConnection() {
        isRunning = false
        if (::vpnInterface.isInitialized) {
            try {
                vpnInterface.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing vpn interface " + e.message )
            }

        }

    }

    private fun establishedVpnConnection(): Boolean {
        if (!::vpnInterface.isInitialized) {
            var builder = Builder()
                .addAddress(serverIp, 32)
                .addRoute("0.0.0.0", 0)

            val configureIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            vpnInterface = builder
                .setSession("MagmaVpn")
                .setConfigureIntent(configureIntent)
                .establish()!!

            return vpnInterface != null
        } else {
            handler.post(kotlinx.coroutines.Runnable {
                onVpnConnectionSuccess()
                Toast.makeText(this, "VpnConnection Already Established", Toast.LENGTH_SHORT).show()
            })
        }
        return true
    }

    private fun readFromVpnInterface() {
        isRunning = true

        var buffer = ByteBuffer.allocate(32767)

        while (isRunning) {
            try {
                var fileInputStream = FileInputStream(vpnInterface.fileDescriptor)

                var length = fileInputStream.read(buffer.array())

                if (length > 0) {
                    var receiveData = String(buffer.array(), 0, length)

                    var intent = Intent("received_data_from_vpn")

                    intent.putExtra("data", receiveData)

                    sendBroadcast(intent)
                    writeToNetwork(buffer, length)
                }


            } catch (e: Exception) {
                Log.e(TAG, "Error reading data from vpn interface " + e.message)
            }
        }
    }

    private fun writeToNetwork(buffer: ByteBuffer, length: Int) {
        var processData = String(buffer.array(), 0, length)

        try {
            var socket = Socket(serverIp, serverPortNumber.toInt())
            var outputStream = socket.getOutputStream()

            var dataBytes = processData.toByteArray(Charsets.UTF_8)
            outputStream.write(dataBytes)
            outputStream.close()
            socket.close()
        } catch (e: Exception) {
            Log.e(TAG, "error sending data to the server " + e.message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        stopVpnConnection()
    }

    private fun onVpnConnectionSuccess() {
        var intent = Intent(ACTION_VPN_CONNECTED)
        sendBroadcast(intent)
    }

}
