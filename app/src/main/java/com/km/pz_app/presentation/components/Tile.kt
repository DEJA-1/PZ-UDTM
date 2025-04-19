package com.km.pz_app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.km.pz_app.ui.theme.backgroundSecondary

@Composable
fun Tile(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                color = backgroundSecondary,
                shape = RoundedCornerShape(size = 20.dp)
            ),
    ) {
        content()
    }
}
