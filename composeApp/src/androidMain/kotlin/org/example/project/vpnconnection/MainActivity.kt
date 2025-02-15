package org.example.project.vpnconnection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo.WindowLayout
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.vpnconnection.Connection.VPNConnector
import org.example.project.vpnconnection.Koin.androidModule
import org.example.project.vpnconnection.Service.MyVpnService
import org.example.project.vpnconnection.UI.App
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    private lateinit var vpnDataReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startKoin {
            androidContext(this@MainActivity)
            modules(androidModule)
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Регистрация BroadcastReceiver
        vpnDataReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Получение данных из Intent
                val receivedData = intent.getStringExtra("data")
                Log.d("VPNDataReceiver", "Получены данные: ${receivedData.toString()}")
                // Здесь можно обновить UI или обработать данные
            }
        }

        val intentFilter = IntentFilter("received_data_from_vpn")
        registerReceiver(vpnDataReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(vpnDataReceiver)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}