package org.example.project.vpnconnection.Connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class AndroidNetworkMonitor(private val context: Context) : NetworkMonitor {

    override fun isConnected(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

    }
}