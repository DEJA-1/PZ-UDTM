package com.km.pz_app.presentation.home

import androidx.annotation.ContentView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.loader.content.Loader

@Composable
fun HomeScreen(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.isError -> Text("Something went wrong")
            else -> Content(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun Content(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        state.cpuPercentUsed?.let {
            CpuUsageBar(usagePercent = it)
        } ?: Text("CPU Usage: --%")
    }
}

@Composable
fun CpuUsageBar(usagePercent: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "CPU Usage: ${"%.1f".format(usagePercent)}%")
        LinearProgressIndicator(
            progress = usagePercent / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}
