package com.km.pz_app.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.km.pz_app.presentation.utils.IpVisualTransformation
import com.km.pz_app.ui.theme.PZAPPTheme
import com.km.pz_app.ui.theme.background
import com.km.pz_app.ui.theme.blue
import com.km.pz_app.ui.theme.textWeak

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RaspberryPiSelector(
    count: Int,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onAddSubmit: (String) -> Unit,
    isError: Boolean = false,
    inputValue: String = "",
    onInputValueChange: (String) -> Unit = {},
    showAdd: Boolean = false,
    modifier: Modifier = Modifier,
    inputEnabled: Boolean = true,
) {
    var isAdding by remember { mutableStateOf(value = false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        AnimatedContent(
            targetState = isAdding,
            label = "selectorToInput",
        ) { adding ->
            if (!adding) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                    modifier = Modifier
                        .horizontalScroll(state = rememberScrollState())
                        .padding(all = 4.dp)
                ) {
                    repeat(count) { index ->
                        val isSelected = index == selectedIndex

                        val animatedBackgroundColor by animateColorAsState(
                            targetValue = if (isSelected) blue.copy(alpha = 0.99f) else Color.Transparent,
                            animationSpec = tween(durationMillis = 600),
                            label = "selectorBackground",
                        )

                        val textColor = if (isSelected) Color.White else textWeak
                        val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(size = 48.dp)
                                .clip(shape = CircleShape)
                                .background(color = animatedBackgroundColor)
                                .clickable { onSelect(index) }
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) blue else textWeak.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = textColor,
                                fontWeight = fontWeight,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    if (showAdd) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(size = 48.dp)
                                .clip(shape = CircleShape)
                                .background(color = Color.Transparent)
                                .clickable {
                                    isAdding = true
                                }
                                .border(
                                    width = 1.dp,
                                    color = blue,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Dodaj",
                                tint = blue
                            )
                        }
                    }
                }
            } else {
                Input(
                    inputValue = inputValue,
                    enabled = inputEnabled,
                    onValueChange = onInputValueChange,
                    visualTransformation = IpVisualTransformation,
                    placeholder = "Wprowadź ip",
                    isError = isError,
                    onSubmitClick = {
                        if (!isError && !inputValue.isBlank()) {
                            onAddSubmit(inputValue)
                            isAdding = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun Preview() {
    PZAPPTheme {
        RaspberryPiSelector(
            count = 4,
            selectedIndex = 2,
            onSelect = {},

            onAddSubmit = {},
            showAdd = false,
            modifier = Modifier.background(color = background)
        )
    }
}