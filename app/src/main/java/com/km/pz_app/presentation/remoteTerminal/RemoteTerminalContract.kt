package com.km.pz_app.presentation.remoteTerminal

import com.km.pz_app.presentation.utils.Resource

data class RemoteTerminalState(
    val inputValue: String,
    val response: Resource<String>,
)

sealed interface RemoteTerminalEvent {
    data class InputValueChange(val newValue: String) : RemoteTerminalEvent
    data object SubmitClick : RemoteTerminalEvent
}

sealed interface RemoteTerminalEffect {

}