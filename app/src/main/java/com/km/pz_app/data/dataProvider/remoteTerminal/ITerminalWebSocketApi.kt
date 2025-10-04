package com.km.pz_app.data.dataProvider.remoteTerminal

import kotlinx.coroutines.flow.SharedFlow

interface ITerminalWebSocketApi {
    val messages: SharedFlow<String>
    val connectivity: SharedFlow<WebSocketStatus>

    fun connect()
    fun send(command: String): Boolean
    fun close(code: Int = 1000, reason: String = "Normal Closure")
}

sealed interface WebSocketStatus {
    data object Open : WebSocketStatus
    data class Closed(val code: Int, val reason: String) : WebSocketStatus
    data class Failure(val message: String?) : WebSocketStatus
}