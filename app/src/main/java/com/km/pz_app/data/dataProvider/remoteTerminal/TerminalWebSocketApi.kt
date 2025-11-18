package com.km.pz_app.data.dataProvider.remoteTerminal

import com.km.pz_app.data.dataProvider.RaspberryAddressProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalWebSocketApi @Inject constructor(
    private val okHttp: OkHttpClient,
) : ITerminalWebSocketApi {
    private var ws: WebSocket? = null

    private val _messages = MutableSharedFlow<String>()
    override val messages: SharedFlow<String> = _messages

    private val _connectivity = MutableSharedFlow<WebSocketStatus>(
        replay = 0, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val connectivity: SharedFlow<WebSocketStatus> = _connectivity

    override fun connect(url: String) {
        ws?.cancel()

        val request = Request.Builder()
            .url(url = url)
            .build()

        ws = okHttp.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectivity.tryEmit(WebSocketStatus.Open)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                _messages.tryEmit(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                _messages.tryEmit(bytes.utf8())
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectivity.tryEmit(WebSocketStatus.Closed(code, reason))
                ws = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectivity.tryEmit(WebSocketStatus.Failure(t.message))
                ws = null
            }
        }
        )
    }

    override fun send(command: String): Boolean = ws?.send(command) ?: false

    override fun close(code: Int, reason: String) {
        ws?.close(code, reason)
        ws = null
    }
}
