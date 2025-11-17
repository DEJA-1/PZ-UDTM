package com.km.pz_app.presentation.remoteTerminal

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.km.pz_app.presentation.components.Input
import com.km.pz_app.presentation.components.RaspberryPiSelector
import com.km.pz_app.presentation.components.Tile
import com.km.pz_app.presentation.home.HomeEvent
import com.km.pz_app.presentation.nav.Destination
import com.km.pz_app.presentation.utils.Resource
import com.km.pz_app.presentation.utils.showToast
import com.km.pz_app.ui.theme.PZAPPTheme
import com.km.pz_app.ui.theme.background
import com.km.pz_app.ui.theme.backgroundSecondary
import com.km.pz_app.ui.theme.backgroundTertiary
import com.km.pz_app.ui.theme.blue
import com.km.pz_app.ui.theme.tertiary
import com.km.pz_app.ui.theme.text
import com.km.pz_app.ui.theme.textWeak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

fun NavGraphBuilder.remoteTerminalScreen() {
    composable<Destination.RemoteTerminal> {
        val viewModel = hiltViewModel<RemoteTerminalViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value

        RemoteTerminalScreen(
            state = state,
            onEvent = viewModel::onEvent,
            effectFlow = viewModel.effectFlow,
        )
    }
}

@Composable
private fun RemoteTerminalScreen(
    state: RemoteTerminalState,
    onEvent: (RemoteTerminalEvent) -> Unit,
    effectFlow: Flow<RemoteTerminalEffect>,
) {
    val context = LocalContext.current

    observeEffect(
        effectFlow = effectFlow,
        context = context,
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(color = background)
            .padding(horizontal = 24.dp, vertical = 48.dp)
    ) {
        var raspberryPiSelected by rememberSaveable { mutableIntStateOf(0) }

        RaspberryPiSelector(
            count = state.raspberrysCount,
            selectedIndex = raspberryPiSelected,
            onSelect = {
                raspberryPiSelected = it
                onEvent(RemoteTerminalEvent.RaspberryIndexChange(it))
            },
            onAddSubmit = {},
            inputEnabled = false,
            modifier = Modifier.fillMaxWidth()
        )

        Input(
            inputValue = state.inputValue,
            enabled = state.isConnected,
            placeholder = getPlaceholder(state.isConnected),
            onValueChange = { onEvent(RemoteTerminalEvent.InputValueChange(it)) },
            onSubmitClick = { onEvent(RemoteTerminalEvent.SubmitClick) }
        )

        Spacer(modifier = Modifier.height(height = 16.dp))

        Tile(modifier = Modifier.fillMaxSize()) {
            if (state.isConnecting) {
                Connecting()
            } else {
                ResponseContent(state)
            }
        }
    }
}

@Composable
private fun ResponseContent(state: RemoteTerminalState) {
    when (state.response) {
        Resource.Loading -> ResponseLoading()
        is Resource.Error -> ResponseError()
        is Resource.Success -> ResponseSuccess(state.response)
    }
}

@Composable
private fun ResponseSuccess(response: Resource.Success<String>) {
    Text(
        text = response.data,
        color = text,
        modifier = Modifier
            .padding(all = 24.dp)
            .verticalScroll(state = rememberScrollState())
    )
}

@Composable
private fun ResponseError() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Coś poszło nie tak",
            color = text,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ResponseLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = text)
    }
}

@Composable
private fun Connecting() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nawiązywanie połączenia z serwerem..",
            color = text,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun observeEffect(
    effectFlow: Flow<RemoteTerminalEffect>,
    context: Context
) {
    LaunchedEffect(Unit) {
        effectFlow.collect { effect ->
            when (effect) {
                is RemoteTerminalEffect.ShowToast -> showToast(
                    context = context,
                    text = effect.message
                )
            }
        }
    }
}

@Composable
private fun getPlaceholder(enabled: Boolean) = if (enabled) {
    "Wprowadź polecenie"
} else {
    "Poczekaj na połączenie z serwerem"
}

@Composable
@Preview(showBackground = true)
private fun Preview() {
    PZAPPTheme {
        RemoteTerminalScreen(
            state = RemoteTerminalState(
                inputValue = "",
                response = Resource.Success("avbcd"),
                isConnecting = false,
                isConnected = true,
                raspberrysCount = 2,
            ),
            onEvent = {},
            effectFlow = emptyFlow()
        )
    }
}