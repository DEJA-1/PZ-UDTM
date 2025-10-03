package com.km.pz_app.presentation.remoteTerminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.km.pz_app.presentation.nav.Destination
import com.km.pz_app.ui.theme.background
import com.km.pz_app.ui.theme.text

fun NavGraphBuilder.remoteTerminalScreen() {
    composable<Destination.RemoteTerminal> {
        val viewModel = hiltViewModel<RemoteTerminalViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value

        RemoteTerminalScreen(
            state = state,
            onEvent = viewModel::onEvent
        )
    }
}

@Composable
private fun RemoteTerminalScreen(
    state: RemoteTerminalState,
    onEvent: (RemoteTerminalEvent) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(color = background)
            .padding(horizontal = 24.dp, vertical = 48.dp)
    ) {
        Text(
            text = "RemoteTerminal screen",
            color = text,
        )
    }
}