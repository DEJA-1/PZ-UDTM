package com.km.pz_app.data.dataProvider.remoteTerminal

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalWebSocketApi @Inject constructor(
    private val okHttp: OkHttpClient,
) : ITerminalWebSocketApi {

    private var ws: WebSocket? = null
    private var currentConnectionId: Long = 0L
    private var lastUrl: String? = null

    private val _messages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val messages: SharedFlow<String> = _messages

    private val _connectivity = MutableSharedFlow<WebSocketStatus>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val connectivity: SharedFlow<WebSocketStatus> = _connectivity

    override fun connect(url: String) {
        ws?.cancel()

        currentConnectionId++
        val connectionIdSnapshot = currentConnectionId
        lastUrl = url

        val request = Request.Builder()
            .url(url)
            .build()

        ws = okHttp.newWebSocket(
            request,
            object : WebSocketListener() {

                private fun isOld(): Boolean =
                    connectionIdSnapshot != currentConnectionId

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (isOld()) {
                        webSocket.close(1000, "Old connection")
                        return
                    }
                    _connectivity.tryEmit(WebSocketStatus.Open)
                    webSocket.send("\n")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (isOld()) return
                    _messages.tryEmit(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    if (isOld()) return
                    val s = bytes.utf8()
                    _messages.tryEmit(s)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (isOld()) return
                    _connectivity.tryEmit(WebSocketStatus.Closed(code, reason))
                    if (ws === webSocket) {
                        ws = null
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (isOld()) return
                    _connectivity.tryEmit(WebSocketStatus.Failure(t.message))
                    if (ws === webSocket) {
                        ws = null
                    }
                }
            }
        )
    }

    override fun send(command: String): Boolean {
        return ws?.send(command) ?: false
    }

    override fun close(code: Int, reason: String) {
        ws?.close(code, reason)
        ws = null
    }
}