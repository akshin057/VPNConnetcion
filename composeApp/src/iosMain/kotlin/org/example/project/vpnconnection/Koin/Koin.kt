package org.example.project.vpnconnection.Koin

import org.example.project.vpnconnection.Connection.IOSNetworkMonitor
import org.example.project.vpnconnection.Connection.IOSVPNConnector
import org.example.project.vpnconnection.Connection.NetworkMonitor
import org.example.project.vpnconnection.Connection.VPNConnector
import org.koin.dsl.module

val iosModule = module {
    single<NetworkMonitor> {IOSNetworkMonitor()}
    single<VPNConnector> {IOSVPNConnector()}
}