package com.km.pz_app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.km.pz_app.ui.theme.backgroundSecondary
import com.km.pz_app.ui.theme.backgroundTertiary
import com.km.pz_app.ui.theme.blue
import com.km.pz_app.ui.theme.tertiary
import com.km.pz_app.ui.theme.text
import com.km.pz_app.ui.theme.textWeak

@Composable
fun Input(
    inputValue: String,
    enabled: Boolean,
    placeholder: String?,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    onValueChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
) {
    val placeholder = placeholder ?: getHomePlaceholder(enabled = enabled)

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
        isError = isError,
        visualTransformation = visualTransformation,
        supportingText = {
            if (isError) {
                Text(
                    text = "Niepoprawne ip",
                    color = tertiary
                )
            }
        },
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
private fun getHomePlaceholder(enabled: Boolean) = if (enabled) {
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
    errorTextColor = tertiary,
)
