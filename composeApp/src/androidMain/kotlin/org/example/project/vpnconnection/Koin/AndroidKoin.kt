package org.example.project.vpnconnection.Koin

import android.content.Context
import org.example.project.vpnconnection.Connection.AndroidNetworkMonitor
import org.example.project.vpnconnection.Connection.AndroidVPNConnector
import org.example.project.vpnconnection.Connection.NetworkMonitor
import org.example.project.vpnconnection.Connection.VPNConnector
import org.koin.dsl.module

val androidModule = module {
    single<NetworkMonitor> {AndroidNetworkMonitor(get<Context>())}
    single<VPNConnector> {AndroidVPNConnector(get<Context>())}
}

