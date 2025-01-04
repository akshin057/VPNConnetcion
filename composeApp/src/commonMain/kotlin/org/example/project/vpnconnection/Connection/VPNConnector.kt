package org.example.project.vpnconnection.Connection

interface VPNConnector {

    fun connect()

    fun disconnect()

    fun isConnected() : Boolean

}