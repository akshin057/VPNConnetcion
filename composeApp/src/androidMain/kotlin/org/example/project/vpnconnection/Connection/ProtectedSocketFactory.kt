package org.example.project.vpnconnection.Connection

import android.net.VpnService
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import javax.net.SocketFactory

class ProtectedSocketFactory(private val vpnService: VpnService) : SocketFactory() {
    private val defaultFactory: SocketFactory = SocketFactory.getDefault()

    override fun createSocket(): Socket {
        val socket = defaultFactory.createSocket()
        vpnService.protect(socket)
        return socket
    }

    override fun createSocket(host: String?, port: Int): Socket {
        val socket = defaultFactory.createSocket(host, port)
        vpnService.protect(socket)
        return socket
    }

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket {
        val socket = defaultFactory.createSocket(host, port, localHost, localPort)
        vpnService.protect(socket)
        return socket
    }

    override fun createSocket(host: InetAddress?, port: Int): Socket {
        val socket = defaultFactory.createSocket(host, port)
        vpnService.protect(socket)
        return socket
    }

    override fun createSocket(host: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket {
        val socket = defaultFactory.createSocket(host, port, localAddress, localPort)
        vpnService.protect(socket)
        return socket
    }
}
