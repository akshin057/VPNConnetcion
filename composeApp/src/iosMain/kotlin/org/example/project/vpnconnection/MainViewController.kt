package org.example.project.vpnconnection

import androidx.compose.ui.window.ComposeUIViewController
import org.example.project.vpnconnection.Koin.iosModule
import org.example.project.vpnconnection.UI.App
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {

//    startKoin{
//        modules(iosModule)
//    }

    App()
}