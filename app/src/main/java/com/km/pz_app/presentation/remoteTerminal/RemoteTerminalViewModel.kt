package com.km.pz_app.presentation.remoteTerminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.km.pz_app.data.dataProvider.RaspberryAddressProvider
import com.km.pz_app.data.dataProvider.remoteTerminal.WebSocketStatus
import com.km.pz_app.data.repository.SelectedRaspberryRepository
import com.km.pz_app.domain.repository.IWebSocketRepository
import com.km.pz_app.presentation.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RemoteTerminalViewModel @Inject constructor(
    private val webSocketRepository: IWebSocketRepository,
    private val raspberryRepository: SelectedRaspberryRepository,
    private val raspberryAddressProvider: RaspberryAddressProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(
        RemoteTerminalState(
            inputValue = "",
            response = Resource.Success(""),
            isConnected = false,
            isConnecting = true,
            raspberrysCount = raspberryAddressProvider.getCount()
        )
    )
    val state = _state.asStateFlow()

    private val effectChannel: Channel<RemoteTerminalEffect> = Channel(Channel.BUFFERED)
    val effectFlow = effectChannel.receiveAsFlow()

    private var connectJob: Job? = null

    init {
        startConnection()
        observeWebSocket()
    }

    fun onEvent(event: RemoteTerminalEvent) {
        when (event) {
            RemoteTerminalEvent.SubmitClick -> handleSubmitClick()
            is RemoteTerminalEvent.InputValueChange -> handleInputValueChange(newValue = event.newValue)
            is RemoteTerminalEvent.RaspberryIndexChange -> handleRaspberryIndexChange(index = event.index)
        }
    }

    private fun startConnection() {
        connectJob?.cancel()

        _state.update {
            it.copy(
                isConnected = false,
                isConnecting = true,
                response = Resource.Success("")
            )
        }

        connectJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                webSocketRepository.terminalClose()
            } catch (_: Exception) {
            }

            val url = raspberryAddressProvider.wsUrl()

            webSocketRepository.terminalConnect(url = url)
        }
    }

    private fun handleInputValueChange(newValue: String) {
        _state.update { it.copy(inputValue = newValue) }
    }

    private fun handleSubmitClick() {
        val prompt = state.value.inputValue.trim()
        if (prompt.isBlank()) return

        _state.update {
            it.copy(
                inputValue = "",
                response = Resource.Loading
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val sentSuccessfully = webSocketRepository.terminalSend("$prompt\n")
            if (!sentSuccessfully) {
                withContext(Dispatchers.Main.immediate) {
                    effectChannel.trySend(RemoteTerminalEffect.ShowToast("Brak połączenia"))
                    _state.update { current ->
                        current.copy(response = Resource.Error("Brak połączenia"))
                    }
                }
            }
        }
    }

    private fun handleRaspberryIndexChange(index: Int) {
        viewModelScope.launch {
            raspberryRepository.setSelectedIndex(index)
            startConnection()
        }
    }

    private fun observeWebSocket() {
        observeResponse()
        observeConnectivity()
    }

    private fun observeResponse() {
        viewModelScope.launch(Dispatchers.Default) {
            webSocketRepository.terminalMessages
                .map { raw -> sanitizeAnsi(raw) }
                .collect { message ->
                    _state.update { current ->
                        val prev = current.response.getResultOrNull().orEmpty()
                        val combined = buildString {
                            append(prev)
                            append(message)
                        }
                        current.copy(response = Resource.Success(combined))
                    }
                }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            webSocketRepository.terminalConnectivity
                .collect { status ->
                    when (status) {
                        WebSocketStatus.Open -> handleOpenStatus()
                        is WebSocketStatus.Closed -> handleClosedStatus()
                        is WebSocketStatus.Failure -> handleFailureStatus(status)
                    }
                }
        }
    }

    private suspend fun handleOpenStatus() {
        _state.update { it.copy(isConnected = true, isConnecting = false) }
        effectChannel.send(RemoteTerminalEffect.ShowToast("Połączono z terminalem"))
    }

    private suspend fun handleClosedStatus() {
        _state.update { it.copy(isConnected = false, isConnecting = false) }
        effectChannel.send(RemoteTerminalEffect.ShowToast("Rozłączono z terminalem"))
    }

    private fun handleFailureStatus(status: WebSocketStatus.Failure) {
        _state.update {
            it.copy(
                isConnected = false,
                response = Resource.Error(status.message ?: "Błąd WS"),
                isConnecting = false
            )
        }
    }

    private fun sanitizeAnsi(input: String?): String {
        if (input.isNullOrEmpty()) return input.orEmpty()
        var s = input.replace("\r\n", "\n")

        val oscRegex = Regex("""\u001B\][0-9]{1,4};[^\u0007\u001B]*?(\u0007|\u001B\\)""")
        s = oscRegex.replace(s, "")

        val csiRegex = Regex("""\u001B\[[0-9;?]*[ -/]*[@-~]""")
        s = csiRegex.replace(s, "")

        val sosPmApcRegex = Regex("""\u001B_[\s\S]*?\u001B\\""")
        s = sosPmApcRegex.replace(s, "")

        s = s.replace("\u0007", "")

        return s
    }

    override fun onCleared() {
        super.onCleared()
        connectJob?.cancel()
        webSocketRepository.terminalClose()
    }
}