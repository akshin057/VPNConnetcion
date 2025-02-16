package org.example.project.vpnconnection.Service

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Authenticator
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.example.project.vpnconnection.Connection.ProtectedSocketFactory
import java.io.FileInputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.Socket
import java.net.URI
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.concurrent.TimeUnit
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
    private lateinit var username: String
    private lateinit var password: String
    private val ACTION_VPN_CONNECTED = "org.example.project.vpnconnection.Service"
    private lateinit var instance: MyVpnService

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    fun getVpnInterface(): ParcelFileDescriptor {
        return vpnInterface
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            serverIp = intent.getStringExtra("vpnIp").toString()
            serverPortNumber = intent.getStringExtra("vpnPort").toString()
            username = intent.getStringExtra("username").toString()
            password = intent.getStringExtra("password").toString()

            serviceScope.launch {
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
                Log.e(TAG, "Error closing vpn interface " + e.message)
            }
        }
    }

    private fun establishedVpnConnection(): Boolean {
        if (!::vpnInterface.isInitialized) {
            val builder = Builder()
                .setSession("MagmaVPN")
                .addAddress("10.0.0.2", 32)  // Локальный IP для VPN
                .addRoute(serverIp, 32) // Исключаем прокси из маршрутизации через VPN
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")

            Log.d(TAG, "$serverIp порт: $serverPortNumber")

            val dummyIntent = Intent()
            val dummyPendingIntent = PendingIntent.getActivity(
                this,
                0,
                dummyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            vpnInterface = builder.setConfigureIntent(dummyPendingIntent).establish()!!

            Log.d(TAG, "VPN established with routes and DNS servers")
            return true
        } else {
            Log.d(TAG, "VPN Connection is already established")
            return true
        }
    }

    private fun readFromVpnInterface() {
        isRunning = true

        var buffer = ByteBuffer.allocate(32767)

        while (isRunning) {
            try {

                val fileInputStream = FileInputStream(vpnInterface.fileDescriptor)
                buffer.clear()
                val length = fileInputStream.read(buffer.array())

                if (length > 0) {
                    val receiveData = buffer.array().copyOf(length)
                    val intent = Intent("received_data_from_vpn")
                    intent.putExtra("data", receiveData)
                    sendBroadcast(intent)
                    writeToNetwork(ByteBuffer.wrap(receiveData), length)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading data from vpn interface " + e.message)
            }
        }
    }

    private fun writeToNetwork(buffer: ByteBuffer, length: Int) {
        val processData = String(buffer.array().copyOf(length))
        try {
            // Создаем объект Proxy с адресом и портом прокси-сервера
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(serverIp, serverPortNumber.toInt()))

            // Создаем OkHttpClient с кастомной SocketFactory, прокси и прокси-аутентификатором
            val client = OkHttpClient.Builder()
                .proxy(proxy)
                .socketFactory(ProtectedSocketFactory(this)) // Здесь "this" — это ваш VpnService
                .proxyAuthenticator { _, response ->
                    response.request.newBuilder()
                        .header("Proxy-Authorization", Credentials.basic(username, password))
                        .build()
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            // Пример запроса для проверки через api.ipify.org
            val request = Request.Builder()
                .url("https://api.ipify.org")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Unexpected response code: ${response.code}")
                } else {
                    Log.d(TAG, "Response: ${response.body?.string()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending data via OkHttp: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        stopVpnConnection()
    }

}

