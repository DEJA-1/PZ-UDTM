package com.km.pz_app.data.repository

import com.km.pz_app.data.dataProvider.remoteTerminal.ITerminalWebSocketApi
import com.km.pz_app.data.dataProvider.remoteTerminal.WebSocketStatus
import com.km.pz_app.domain.repository.IWebSocketRepository
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

class WebSocketRepository @Inject constructor(
    private val api: ITerminalWebSocketApi
) : IWebSocketRepository {
    override fun terminalConnect() = api.connect()
    override fun terminalSend(command: String): Boolean = api.send(command)
    override fun terminalClose() = api.close()

    override val terminalMessages: SharedFlow<String> = api.messages
    override val terminalConnectivity: SharedFlow<WebSocketStatus> = api.connectivity
}