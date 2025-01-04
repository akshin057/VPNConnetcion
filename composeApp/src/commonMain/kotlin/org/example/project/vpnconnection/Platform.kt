package org.example.project.vpnconnection

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform