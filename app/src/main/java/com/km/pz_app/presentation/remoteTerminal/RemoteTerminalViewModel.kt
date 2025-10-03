package com.km.pz_app.presentation.remoteTerminal

import androidx.lifecycle.ViewModel
import com.km.pz_app.presentation.nav.navigator.INavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class RemoteTerminalViewModel @Inject constructor(
    private val navigator: INavigator,
) : ViewModel() {
    private val _state = MutableStateFlow(
        RemoteTerminalState(inputValue = "")
    )
    val state = _state.asStateFlow()

    fun onEvent(event: RemoteTerminalEvent) {
        when (event) {
            is RemoteTerminalEvent.InputValueChange -> handleInputValueChange(
                newValue = event.newValue
            )
        }
    }

    private fun handleInputValueChange(newValue: String) {
        _state.update { it.copy(inputValue = newValue) }
    }
}