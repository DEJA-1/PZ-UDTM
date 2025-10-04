package com.km.pz_app.presentation.remoteTerminal

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.km.pz_app.presentation.components.Tile
import com.km.pz_app.presentation.nav.Destination
import com.km.pz_app.presentation.utils.Resource
import com.km.pz_app.ui.theme.background
import com.km.pz_app.ui.theme.backgroundSecondary
import com.km.pz_app.ui.theme.backgroundTertiary
import com.km.pz_app.ui.theme.blue
import com.km.pz_app.ui.theme.tertiary
import com.km.pz_app.ui.theme.text
import com.km.pz_app.ui.theme.textWeak
import kotlinx.coroutines.flow.Flow

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
        verticalArrangement = Arrangement.spacedBy(space = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(color = background)
            .padding(horizontal = 24.dp, vertical = 84.dp)
    ) {
        Input(
            inputValue = state.inputValue,
            enabled = state.isConnected,
            onValueChange = { onEvent(RemoteTerminalEvent.InputValueChange(it)) },
            onSubmitClick = { onEvent(RemoteTerminalEvent.SubmitClick) }
        )

        Tile(modifier = Modifier.fillMaxSize()) {
            if (state.isConnecting) {
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
            } else {
                when (state.response) {
                    Resource.Loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = text)
                    }

                    is Resource.Error -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Coś poszło nie tak",
                            color = text,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    is Resource.Success -> Text(
                        text = state.response.data,
                        color = text,
                        modifier = Modifier
                            .padding(all = 24.dp)
                            .verticalScroll(state = rememberScrollState())
                    )
                }
            }
        }
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
                is RemoteTerminalEffect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun Input(
    inputValue: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
) {
    val placeholder = getPlaceholder(enabled = enabled)

    OutlinedTextField(
        value = inputValue,
        onValueChange = onValueChange,
        label = { Text(text = placeholder) },
        colors = getInputColors(),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        singleLine = true,
        readOnly = !enabled,
        trailingIcon = {
            AnimatedVisibility(
                visible = inputValue.isNotBlank(),
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.padding(all = 8.dp)
            ) {
                FilledIconButton(
                    onClick = onSubmitClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = blue,
                        contentColor = text,
                    ),
                    modifier = Modifier.size(size = 40.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Wyślij"
                    )
                }
            }
        }
    )
}

@Composable
private fun
        getPlaceholder(enabled: Boolean) = if (enabled) {
    "Wprowadź polecenie"
} else {
    "Poczekaj na połączenie z serwerem"
}

@Composable
private fun getInputColors() = OutlinedTextFieldDefaults.colors().copy(
    focusedContainerColor = backgroundSecondary,
    unfocusedContainerColor = backgroundSecondary,
    disabledContainerColor = backgroundSecondary.copy(alpha = 0.5f),
    focusedTextColor = text,
    unfocusedTextColor = text,
    disabledTextColor = textWeak,
    focusedPlaceholderColor = textWeak,
    unfocusedPlaceholderColor = textWeak,
    focusedLabelColor = blue,
    unfocusedLabelColor = textWeak,
    errorLabelColor = tertiary,
    cursorColor = blue,
    errorCursorColor = tertiary,
    textSelectionColors = TextSelectionColors(
        handleColor = blue,
        backgroundColor = blue.copy(alpha = 0.25f)
    ),
    focusedIndicatorColor = blue,
    unfocusedIndicatorColor = backgroundTertiary,
    disabledIndicatorColor = backgroundTertiary.copy(alpha = 0.5f),
    errorIndicatorColor = tertiary,
    focusedLeadingIconColor = text,
    unfocusedLeadingIconColor = textWeak,
    disabledLeadingIconColor = textWeak.copy(alpha = 0.5f),
    errorLeadingIconColor = tertiary,
    focusedTrailingIconColor = text,
    unfocusedTrailingIconColor = textWeak,
    disabledTrailingIconColor = textWeak.copy(alpha = 0.5f),
    errorTrailingIconColor = tertiary,
    focusedPrefixColor = textWeak,
    unfocusedPrefixColor = textWeak,
    disabledPrefixColor = textWeak.copy(alpha = 0.5f),
    errorPrefixColor = tertiary,
    focusedSuffixColor = textWeak,
    unfocusedSuffixColor = textWeak,
    disabledSuffixColor = textWeak.copy(alpha = 0.5f),
    errorSuffixColor = tertiary,
)