package com.km.pz_app.domain.repository

import com.km.pz_app.data.dataProvider.remoteTerminal.WebSocketStatus
import kotlinx.coroutines.flow.SharedFlow

interface IWebSocketRepository {
    fun terminalConnect()
    fun terminalSend(command: String): Boolean
    fun terminalClose()
    val terminalMessages: SharedFlow<String>
    val terminalConnectivity: SharedFlow<WebSocketStatus>
}