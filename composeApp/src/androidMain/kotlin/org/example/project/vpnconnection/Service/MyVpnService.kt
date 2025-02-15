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
import okhttp3.Call
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.FileInputStream
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.UUID
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

//    private fun testDnsResolution(domain: String) {
//        try {
//            val addresses = InetAddress.getAllByName(domain)
//            addresses.forEach {
//                Log.d(TAG, "Resolved $domain to IP: ${it.hostAddress}")
//            }
//        } catch (e: UnknownHostException) {
//            Log.e(TAG, "Failed to resolve $domain: ${e.message}")
//        }
//    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            serverIp = intent.getStringExtra("vpnIp").toString()
            serverPortNumber = intent.getStringExtra("vpnPort").toString()
            uuid = intent.getStringExtra("uuid").toString()
            domain = intent.getStringExtra("domain").toString()
            path = intent.getStringExtra("path").toString()
            security = intent.getStringExtra("security").toString()

            serviceScope.launch {
                runVpnConnection()
            }
        }
        return START_STICKY
    }

    private fun runVpnConnection() {
        try {
            if (establishedVpnConnection()) {
//                startTun2socks()
//                startV2RayCore()
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
                .addAddress(serverIp, 32)
                .addRoute("0.0.0.0", 1)

            vpnInterface = builder.establish()!!

            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface")
                return false
            }

            Log.d(TAG, "VPN established with routes and DNS servers")
            return true
        }
        return true
    }

//    private fun establishedVpnConnection(): Boolean {
//        if (!::vpnInterface.isInitialized) {
//            val builder = Builder()
//                .addAddress("10.0.0.2", 32)
//                .addRoute("0.0.0.0", 0)
//                .addDnsServer("8.8.8.8")
//
//            val configureIntent = PendingIntent.getActivity(
//                applicationContext,
//                0,
//                Intent(),
//                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
//            )
//
//            vpnInterface = builder
//                .setSession("MagmaVpn")
//                .setConfigureIntent(configureIntent)
//                .establish()!!
//
//            return vpnInterface != null
//        } else {
//            handler.post {
//                onVpnConnectionSuccess()
//                Toast.makeText(
//                    this,
//                    "VpnConnection Already Established",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//        return true
//    }

//    private fun startTun2socks() {
//        try {
//            val tun2socksPath = applicationContext.filesDir.absolutePath + "/tun2socks"
//            val command = arrayOf(
//                tun2socksPath,
//                "--netif-ipaddr", "10.10.10.1",
//                "--netif-netmask", "255.255.255.0",
//                "--socks-server-addr", "127.0.0.1:10808",
//                "--tunfd", vpnInterface.fileDescriptor.toString()
//            )
//
//            ProcessBuilder(*command).start()
//            Log.d(TAG, "tun2socks started")
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to start tun2socks: ${e.message}")
//        }
//    }
//
//    private fun startV2RayCore() {
//        try {
//            val v2rayPath = applicationContext.filesDir.absolutePath + "/v2ray"
//            val configPath = applicationContext.filesDir.absolutePath + "/v2ray_config.json"
//
//            val command = arrayOf(
//                v2rayPath,
//                "-config", configPath
//            )
//
//            ProcessBuilder(*command).start()
//            Log.d(TAG, "V2Ray core started")
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to start V2Ray core: ${e.message}")
//        }
//    }

    private fun readFromVpnInterface() {
        isRunning = true

        var buffer = ByteBuffer.allocate(32767)

        try {
            val address = InetAddress.getByName(domain)
            Log.d("TEST", "Resolved: ${address.hostAddress}")
        } catch (e: UnknownHostException) {
            Log.e("TEST", "Failed: ${e.message}")
        }

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

    private fun convertUuidStringTo16Bytes(uuidString: String): ByteArray {
        val u = UUID.fromString(uuidString)
        val buffer = ByteBuffer.allocate(16)
        buffer.putLong(u.mostSignificantBits)
        buffer.putLong(u.leastSignificantBits)
        return buffer.array()
    }

    private fun writeToNetwork(buffer: ByteBuffer, length: Int) {
        try {
            val client = OkHttpClient.Builder()
                .sslSocketFactory(createSslSocketFactory(), createTrustManager())
                .build()

            val url = "wss://testserver.work.gd/ioaroistivatxqripdhyeodc"

            val request = Request.Builder()
                .url(url)
                .header("Connection", "Upgrade")
                .header("Upgrade", "websocket")
                .header("Sec-WebSocket-Version", "13")
                .header(
                    "Sec-WebSocket-Key",
                    "7A/LbxXQzy/u9GVlX+aHhw=="
                ) // Можно сгенерировать динамически
                .build()

            val webSocketListener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connection established: $url")

                    val version: Byte = 1
                    val uuidBytes = convertUuidStringTo16Bytes(uuid)
                    val additionalInfo = "Пример".toByteArray(Charsets.UTF_8)
                    val additionalInfoLen = additionalInfo.size.coerceAtMost(255)
                    val command: Byte = 0x01
                    val portBytes = serverPortNumber.toShort().toBigEndianBytes()
                    val addressType: Byte = 0x01
                    val addressBytes = serverIp.toByteArray(Charsets.UTF_8)
                    val requestData = buffer.array().copyOf(length)

                    val totalSize = 1 + 16 + 1 + additionalInfoLen + 1 +
                            2 + 1 + addressBytes.size + requestData.size

                    val requestPayload = ByteBuffer.allocate(totalSize).apply {
                        put(version)
                        put(uuidBytes)
                        put(additionalInfoLen.toByte())
                        put(additionalInfo, 0, additionalInfoLen)
                        put(command)
                        put(portBytes)
                        put(addressType)
                        put(addressBytes)
                        put(requestData)
                    }.array()

                    val byteStringPayload = requestPayload.toByteString()
                    webSocket.send(byteStringPayload)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket connection failed: ${t.message} $response")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "Message: $text")
                }
            }

            client.newWebSocket(request, webSocketListener)
            client.dispatcher.executorService.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending data via WebSocket: ${e.message}")
        }
    }

    private fun createSslSocketFactory(): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(createTrustManager()), SecureRandom())
        return sslContext.socketFactory
    }

    private fun createTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {}
        }
    }

    private fun Short.toBigEndianBytes(): ByteArray {
        return byteArrayOf(
            ((this.toInt() shr 8) and 0xFF).toByte(),
            (this.toInt() and 0xFF).toByte()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        stopVpnConnection()
    }

    private fun onVpnConnectionSuccess() {
        val intent = Intent(ACTION_VPN_CONNECTED)
        sendBroadcast(intent)
    }
}

