package org.example.project.vpnconnection.Connection

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import org.example.project.vpnconnection.Service.MyVpnService

class AndroidVPNConnector(
    private val context: Context
) : VPNConnector {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isConnected = false
    private val TAG = "AndroidVPNConnector"

    override fun connect() {
        val vpnIntent = VpnService.prepare(context)

        if (vpnIntent != null) {
            // Запускаем Activity для запроса разрешения на VPN
            if (context is Activity) {
                context.startActivityForResult(vpnIntent, 1)
            } else {
                Log.e(TAG, "Context is not an instance of Activity")
            }
        } else {
            startVpnServiceWithIp()
        }
    }

    override fun disconnect() {
        stopVpnConnection()
    }

    override fun isConnected(): Boolean {
        return isConnected
    }

    private fun startVpnServiceWithIp() {
        val vpnIntent = Intent(context, MyVpnService::class.java).apply {
            putExtra("vpnIp", "171.22.117.42")
            putExtra("vpnPort", "443")
            putExtra("uuid", "dd75eebb-6a52-431b-b75f-db6e1cfb9673")
            putExtra("domain", "testserver.work.gd")
            putExtra("path", "gmadfzfevlmqdkduwlgkshbl")
            putExtra("security", "tls")
        }
        context.startService(vpnIntent)
    }

    private fun stopVpnConnection() {
        isConnected = false
        vpnInterface?.close()
        vpnInterface = null
    }
}
