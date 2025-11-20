package com.km.pz_app.data.repository

import android.util.Log
import com.km.pz_app.data.dataProvider.RaspberryAddressProvider
import com.km.pz_app.data.dataProvider.remoteTerminal.ITerminalWebSocketApi
import com.km.pz_app.data.dataProvider.remoteTerminal.WebSocketStatus
import com.km.pz_app.domain.repository.IWebSocketRepository
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketRepository @Inject constructor(
    private val api: ITerminalWebSocketApi,
) : IWebSocketRepository {

    override val terminalMessages: SharedFlow<String> = api.messages
    override val terminalConnectivity: SharedFlow<WebSocketStatus> = api.connectivity

    override suspend fun terminalConnect(url: String) {
        api.connect(url = url)
    }

    override fun terminalSend(command: String): Boolean {
        return api.send(command = command)
    }

    override fun terminalClose() {
        api.close(code = 1000, reason = "Normal closure from app")
    }
}