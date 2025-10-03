package com.km.pz_app.presentation.remoteTerminal

data class RemoteTerminalState(
    val inputValue: String
)

sealed interface RemoteTerminalEvent {
    data class InputValueChange(val newValue: String) : RemoteTerminalEvent
}

sealed interface RemoteTerminalEffect {

}