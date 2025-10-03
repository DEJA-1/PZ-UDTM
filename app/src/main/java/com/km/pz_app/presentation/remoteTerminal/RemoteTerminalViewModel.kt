package com.km.pz_app.presentation.remoteTerminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.km.pz_app.presentation.nav.navigator.INavigator
import com.km.pz_app.presentation.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class RemoteTerminalViewModel @Inject constructor(
    private val navigator: INavigator,
) : ViewModel() {
    private val _state = MutableStateFlow(
        RemoteTerminalState(
            inputValue = "",
            response = Resource.Success(""),
        )
    )
    val state = _state.asStateFlow()

    fun onEvent(event: RemoteTerminalEvent) {
        when (event) {
            is RemoteTerminalEvent.InputValueChange -> handleInputValueChange(newValue = event.newValue)
            RemoteTerminalEvent.SubmitClick -> handleSubmitClick()
        }
    }

    private fun handleInputValueChange(newValue: String) {
        _state.update { it.copy(inputValue = newValue) }
    }

    private fun handleSubmitClick() {
        _state.update { it.copy(inputValue = "") }
        fetchResponse()
    }

    private fun fetchResponse() {
        _state.update { it.copy(response = Resource.Loading) }

        viewModelScope.launch(Dispatchers.IO) {
            delay(1.seconds)
            _state.update { it.copy(response = Resource.Success("backend response")) }
        }
    }
}