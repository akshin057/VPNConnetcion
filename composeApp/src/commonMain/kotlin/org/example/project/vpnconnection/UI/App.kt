package org.example.project.vpnconnection.UI

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.vpnconnection.Connection.NetworkMonitor
import org.example.project.vpnconnection.Connection.VPNConnector
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    MaterialTheme {
        MainButton()
    }
}

@Composable
fun MainButton() {

    val networkMonitor: NetworkMonitor = koinInject()
    val vpnConnector: VPNConnector = koinInject()
    val snackbarHostState = remember { SnackbarHostState() }
    var isConnected by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isConnectedText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.LightGray)
    ) {
        TextField(
            value = isConnectedText,
            onValueChange = { isConnectedText = it },
            label = { Text("VPN State") },
            modifier = Modifier.fillMaxWidth()
                .background(Color.Gray)
                .height(150.dp)
        )

        Button(
            onClick = {
                if (networkMonitor.isConnected()) {
                    if (!vpnConnector.isConnected()) {
                        vpnConnector.connect()
                        isConnectedText = "VPN Подключен"
                    } else {
                        vpnConnector.disconnect()
                        isConnectedText = "Ваше соединение не защищено"
                    }
                    isConnected = !isConnected
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Нет подключения к интернету")
                    }
                }
            },
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Text("Подключиться к VPN")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}




