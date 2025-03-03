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
            if (context is Activity) {
                context.startActivityForResult(vpnIntent, 1)
                isConnected = true
            } else {
                Log.e(TAG, "Context is not an instance of Activity")
            }
        } else {
            startVpnServiceWithIp()
            isConnected = true
        }
    }

    override fun disconnect() {
        stopVpnConnection()
        isConnected = false
    }

    override fun isConnected(): Boolean {
        return isConnected
    }

    private fun startVpnServiceWithIp() {
        val vpnIntent = Intent(context, MyVpnService::class.java).apply {
            putExtra("vpnIp", "45.67.230.135")
            putExtra("vpnPort", "3128")
            putExtra("username", "username")
            putExtra("password", "strongpassword")
        }
        context.startService(vpnIntent)
    }

    private fun stopVpnConnection() {
        val intent = Intent(context, MyVpnService::class.java)
        isConnected = false
        vpnInterface?.close()
        vpnInterface = null
        context.stopService(intent)
    }

}
