package com.km.pz_app.presentation.remoteTerminal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontVariation.width
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
import com.km.pz_app.ui.theme.primary
import com.km.pz_app.ui.theme.tertiary
import com.km.pz_app.ui.theme.text
import com.km.pz_app.ui.theme.text
import com.km.pz_app.ui.theme.textWeak

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
        verticalArrangement = Arrangement.spacedBy(space = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(color = background)
            .padding(horizontal = 24.dp, vertical = 84.dp)
    ) {
        Input(
            inputValue = state.inputValue,
            onValueChange = { onEvent(RemoteTerminalEvent.InputValueChange(it)) },
            onSubmitClick = { onEvent(RemoteTerminalEvent.SubmitClick) }
        )

        Tile(modifier = Modifier.fillMaxSize()) {
            when (state.response) {
                Resource.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = text)
                }

                is Resource.Error -> Text(
                    text = "Coś poszło nie tak",
                    color = text,
                    fontWeight = FontWeight.Bold
                )

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

@Composable
private fun Input(
    inputValue: String,
    onValueChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
) {
    OutlinedTextField(
        value = inputValue,
        onValueChange = onValueChange,
        label = { Text(text = "Wprowadź polecenie") },
        colors = getInputColors(),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        singleLine = true,
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
                        containerColor = primary,
                        contentColor = text,
                    ),
                    modifier = Modifier.size(size = 40.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Wyślij"
                    )
                }
            }
        }
    )
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