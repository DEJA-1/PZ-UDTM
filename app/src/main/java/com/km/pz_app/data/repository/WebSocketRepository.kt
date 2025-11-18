package com.km.pz_app.data.repository

import com.km.pz_app.data.dataProvider.RaspberryAddressProvider
import com.km.pz_app.data.dataProvider.remoteTerminal.ITerminalWebSocketApi
import com.km.pz_app.data.dataProvider.remoteTerminal.WebSocketStatus
import com.km.pz_app.domain.repository.IWebSocketRepository
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

class WebSocketRepository @Inject constructor(
    private val api: ITerminalWebSocketApi,
    private val raspberryAddressProvider: RaspberryAddressProvider,
) : IWebSocketRepository {

    override val terminalMessages: SharedFlow<String> = api.messages
    override val terminalConnectivity: SharedFlow<WebSocketStatus> = api.connectivity

    override suspend fun terminalConnect() {
        val url = raspberryAddressProvider.wsUrl()
        api.connect(url = url)
    }

    override suspend fun terminalSend(command: String): Boolean =
        api.send(command = command)

    override fun terminalClose() {
        api.close()
    }
}