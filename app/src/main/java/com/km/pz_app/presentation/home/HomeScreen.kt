package com.km.pz_app.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.CpuStats
import com.km.pz_app.domain.model.CpuUsage
import com.km.pz_app.domain.model.MemoryResponse
import com.km.pz_app.domain.model.ProcessInfo
import com.km.pz_app.domain.model.ProcessResponse
import com.km.pz_app.presentation.utils.Resource
import com.km.pz_app.ui.theme.PZAPPTheme
import com.km.pz_app.ui.theme.background
import com.km.pz_app.ui.theme.backgroundBadgeDisabled
import com.km.pz_app.ui.theme.backgroundBadgeEnabled
import com.km.pz_app.ui.theme.backgroundSecondary
import com.km.pz_app.ui.theme.backgroundTertiary
import com.km.pz_app.ui.theme.primary
import com.km.pz_app.ui.theme.text
import com.km.pz_app.ui.theme.textWeak
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun HomeScreen(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = background)
            .padding(horizontal = 24.dp, vertical = 48.dp)
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
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
    ) {
        state.cpuTemperature?.let {
            TemperatureGauge(
                temperature = it,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RAMTile(
                usedRamPercent = state.usedRamPercent,
                usedRamGb = state.usedRamGb,
                modifier = Modifier.weight(1f)
            )
            CPUTile(
                systemPercent = state.cpuPercentUsed,
                modifier = Modifier.weight(1f)
            )
        }
        state.processes.getResultOrNull()?.let {
            ProcessesTile(processes = it.processes.toPersistentList())
        }
    }
}

@Composable
private fun ProcessesTile(processes: PersistentList<ProcessInfo>) {
    Tile(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            processes.forEach {
                ProcessInfo(info = it)
            }
        }
    }
}

@Composable
private fun ProcessInfo(info: ProcessInfo, cpuPercent: Float? = null) {
    val (badgeColor, badgeTextColor) = info.getBadgeColorAndText()

    val memoryMB = info.memoryRss.toFloat() / 1024f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f, fill = false)
        ) {
            Text(
                text = info.name,
                color = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(height = 4.dp))
            Text(
                text = "${memoryMB.format(1)} MB",
                color = textWeak,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Badge(
            content = {
                Text(
                    text = info.stateDescription,
                    color = badgeTextColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )
            },
            containerColor = badgeColor
        )
    }
}


@Composable
private fun CPUTile(
    systemPercent: Float?,
    modifier: Modifier = Modifier
) {
    Tile(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 32.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "CPU usage",
                color = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(height = 4.dp))
            CPUUsageRow(
                title = "Sys",
                percent = systemPercent,
            )
            CPUUsageRow(
                title = "Idle",
                percent = systemPercent?.let { 100f - it },
            )
        }
    }
}


@Composable
private fun CPUUsageRow(
    title: String,
    percent: Float?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            color = textWeak,
            style = MaterialTheme.typography.bodyMedium
        )
        LinearProgressIndicator(
            color = primary,
            trackColor = backgroundTertiary,
            strokeCap = StrokeCap.Round,
            progress = { (percent ?: 0f) / 100f },
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )
        Text(
            text = percent?.let { "%.0f%%".format(it) } ?: "-",
            color = textWeak,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}


@Composable
private fun RAMTile(
    usedRamPercent: Float?,
    usedRamGb: Pair<Float, Float>?,
    modifier: Modifier = Modifier
) {
    Tile(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)
        ) {
            Text(
                text = "RAM",
                color = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = usedRamGb?.let { "%.1f GB / %.1f GB".format(it.first, it.second) } ?: "-",
                color = textWeak,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                color = primary,
                trackColor = backgroundTertiary,
                strokeCap = StrokeCap.Round,
                progress = { (usedRamPercent ?: 0f) / 100f }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = usedRamPercent?.let { "%.0f%%".format(it) } ?: "-",
                color = textWeak,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun TemperatureGauge(
    temperature: Float,
    modifier: Modifier = Modifier,
    minTemp: Float = 0f,
    maxTemp: Float = 100f,
    activeColor: Color = primary,
    inactiveColor: Color = backgroundTertiary,
) {
    val sweepAngle = ((temperature - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f) * 180f

    Tile(modifier = modifier.aspectRatio(ratio = 1.8f)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Temperature",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray,
            )
            Text(
                text = "${"%.1f".format(temperature)}°C",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(height = 42.dp))
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 42.dp, vertical = 42.dp)
        ) {
            val strokeWidth = size.height / 5f
            val radius = size.width / 2.1f
            val center = Offset(x = size.width / 1.8f, y = size.height / 0.9f)

            drawArc(
                color = inactiveColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 1.8f, radius * 1.8f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color = activeColor,
                startAngle = 180f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 1.8f, radius * 1.8f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun Tile(
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

private fun ProcessInfo.getBadgeColorAndText() = when (this.stateDescription) {
    "sleeping" -> backgroundBadgeDisabled to textWeak
    else -> backgroundBadgeEnabled to text
}

fun Float.format(digits: Int): String =
    "%.${digits}f".format(this).replace('.', ',')


@Composable
@Preview(showBackground = true)
private fun Preview() {
    PZAPPTheme {
        HomeScreen(
            state = HomeState(
                cpu = Resource.Success(
                    CpuResponse(
                        cpuTemperature = 42.5f,
                        cpuUsage = CpuUsage(
                            full = CpuStats(
                                userNorm = 150,
                                userNice = 0,
                                kernel = 100,
                                idle = 3000,
                                ioWait = 2,
                                irq = 0,
                                softIrq = 0
                            ),
                            cores = emptyList()
                        )
                    )
                ),
                memory = Resource.Success(
                    MemoryResponse(
                        total = 3882924,
                        free = 1500000,
                        available = 2900000
                    )
                ),
                processes = Resource.Success(
                    ProcessResponse(
                        processes = listOf(
                            ProcessInfo(
                                pid = 123,
                                name = "systemd",
                                stateCode = "R",
                                stateDescription = "sleeping",
                                user = "1000",
                                group = "1000",
                                memoryRss = 120000,
                                memoryVirt = 300000,
                                swap = 0,
                                threads = 5,
                                uTime = 120
                            ),
                            ProcessInfo(
                                pid = 123,
                                name = "systemd",
                                stateCode = "R",
                                stateDescription = "interrupt",
                                user = "1000",
                                group = "1000",
                                memoryRss = 120000,
                                memoryVirt = 300000,
                                swap = 0,
                                threads = 5,
                                uTime = 120
                            ),
                            ProcessInfo(
                                pid = 123,
                                name = "systemd",
                                stateCode = "R",
                                stateDescription = "running",
                                user = "1000",
                                group = "1000",
                                memoryRss = 120000,
                                memoryVirt = 300000,
                                swap = 0,
                                threads = 5,
                                uTime = 120
                            )
                        )
                    )
                ),
                cpuPercentUsed = 47.5f,
                cpuTemperature = 42.5f,
                usedRamPercent = 25.8f,
                usedRamGb = 1.2f to 3.7f
            ),
            onEvent = {}
        )
    }
}
