package com.km.pz_app.presentation.home

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.CpuStats
import com.km.pz_app.domain.model.CpuUsage
import com.km.pz_app.domain.model.MemoryResponse
import com.km.pz_app.domain.model.ProcessInfo
import com.km.pz_app.domain.model.ProcessResponse
import com.km.pz_app.presentation.components.RaspberryPiSelector
import com.km.pz_app.presentation.components.Tile
import com.km.pz_app.presentation.nav.Destination
import com.km.pz_app.presentation.utils.Resource
import com.km.pz_app.presentation.utils.showToast
import com.km.pz_app.ui.theme.PZAPPTheme
import com.km.pz_app.ui.theme.background
import com.km.pz_app.ui.theme.backgroundBadgeDisabled
import com.km.pz_app.ui.theme.backgroundBadgeEnabled
import com.km.pz_app.ui.theme.backgroundTertiary
import com.km.pz_app.ui.theme.blue
import com.km.pz_app.ui.theme.primary
import com.km.pz_app.ui.theme.secondary
import com.km.pz_app.ui.theme.tertiary
import com.km.pz_app.ui.theme.text
import com.km.pz_app.ui.theme.textWeak
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

fun NavGraphBuilder.homeScreen() {
    composable<Destination.Home> {
        val viewModel = hiltViewModel<HomeViewModel>()
        val state = viewModel.state.collectAsStateWithLifecycle().value

        HomeScreen(
            state = state,
            effectFlow = viewModel.effectFlow,
            onEvent = viewModel::onEvent
        )
    }
}

@Composable
private fun HomeScreen(
    state: HomeState,
    effectFlow: Flow<HomeEffect>,
    onEvent: (HomeEvent) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(color = background)
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(
                color = text
            )

            state.isError -> Text(
                text = "Coś poszło nie tak",
                color = text,
                fontWeight = FontWeight.Bold
            )

            else -> Content(
                state = state,
                effectFlow = effectFlow,
                onEvent = onEvent
            )
        }
    }
}

@Composable
private fun Content(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    effectFlow: Flow<HomeEffect>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var killProcessDialogId: Int? by rememberSaveable {
        mutableStateOf(null)
    }
    var raspberryPiSelected by rememberSaveable { mutableIntStateOf(0) }

    HandleProcessKillResult(
        effectFlow = effectFlow,
        context = context,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
            modifier = modifier
                .padding(horizontal = 24.dp, vertical = 48.dp)
                .padding(bottom = 16.dp)
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
        ) {
            ShowDialog(
                shouldShow = killProcessDialogId != null,
                onConfirm = {
                    onEvent(HomeEvent.ProcessKillClick(killProcessDialogId!!))
                    killProcessDialogId = null
                },
                onCancel = {
                    killProcessDialogId = null
                }
            )

            RaspberryPiSelector(
                count = 3,
                selectedIndex = raspberryPiSelected,
                onSelect = {
                    raspberryPiSelected = it
                    onEvent(HomeEvent.RaspberryIndexChange(it))
                }
            )

            TemperatureChart(state)

            UpperTiles(state)
            state.processes.getResultOrNull()?.let {
                ProcessesTile(
                    processes = it.processes.toPersistentList(),
                    killingProcesses = state.killingProcesses,
                    onDeleteClick = { id ->
                        killProcessDialogId = id
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = { onEvent(HomeEvent.RemoteTerminalClick) },
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(all = 24.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Akcja FAB"
            )
        }
    }
}

@Composable
private fun UpperTiles(state: HomeState) {
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
}

@Composable
private fun TemperatureChart(state: HomeState) {
    var tempType by rememberSaveable { mutableStateOf(TemperatureType.Internal) }

    val temperatureToShow = when (tempType) {
        TemperatureType.Internal -> state.cpuTemperature
        TemperatureType.External -> state.externalTemperature
    }

    temperatureToShow?.let {
        TemperatureGauge(
            temperature = it,
            modifier = Modifier.fillMaxWidth()
        )
    }
    TemperatureTypeSelector(
        selected = tempType,
        onSelect = { tempType = it },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun HandleProcessKillResult(
    effectFlow: Flow<HomeEffect>,
    context: Context,
) {
    LaunchedEffect(Unit) {
        effectFlow.collect {
            when (it) {
                HomeEffect.KillProcessFailure -> showToast(
                    context = context,
                    text = "Ubicie procesu nie udało się!"
                )

                HomeEffect.KillProcessSuccess -> showToast(
                    context = context,
                    text = "Proces ubity!"
                )
            }
        }
    }
}

@Composable
private fun ProcessesTile(
    processes: PersistentList<ProcessInfo>,
    killingProcesses: Set<Int>,
    onDeleteClick: (Int) -> Unit,
) {
    Tile(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            processes.forEach {
                ProcessInfo(
                    info = it,
                    onDeleteClick = { id -> onDeleteClick(id) },
                    isMarkedForKill = killingProcesses.contains(it.pid)
                )
            }
        }
    }
}

@Composable
private fun ProcessInfo(
    info: ProcessInfo,
    onDeleteClick: (Int) -> Unit,
    isMarkedForKill: Boolean,
) {
    val (badgeColor, badgeTextColor) = info.getBadgeColorAndText()
    val memoryMB = info.memoryVirt.toFloat() / 1024f
    val alpha = if (isMarkedForKill) 0.3f else 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxSize()
            .alpha(alpha = alpha)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .weight(weight = 1f, fill = false)
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
                    text = if (isMarkedForKill) "Ubijanie..." else info.stateDescription,
                    color = badgeTextColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )
            },
            containerColor = badgeColor
        )

        DeleteIcon(
            onClick = {
                if (isMarkedForKill) Unit else onDeleteClick(info.pid)
            })
    }
}

@Composable
private fun ShowDialog(
    shouldShow: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    if (shouldShow) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = {
                Text("Potwierdzenie")
            },
            text = {
                Text("Czy na pewno chcesz ubić proces?")
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Tak")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) {
                    Text("Nie")
                }
            }
        )
    }


}

@Composable
private fun DeleteIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(shape = CircleShape)
            .background(color = tertiary.copy(alpha = 0.2f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = tertiary,
            modifier = Modifier.padding(all = 4.dp)
        )
    }
}

@Composable
private fun CPUTile(
    systemPercent: Float?,
    modifier: Modifier = Modifier,
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
                isIdle = false,
            )
            CPUUsageRow(
                title = "Idle",
                percent = systemPercent?.let { 100f - it },
                isIdle = true,
            )
        }
    }
}


@Composable
private fun CPUUsageRow(
    title: String,
    percent: Float?,
    isIdle: Boolean,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (percent ?: 0f) / 100f,
        animationSpec = tween(durationMillis = 600)
    )

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
            color = animatedProgress.progressColor(isIdle = isIdle),
            trackColor = backgroundTertiary,
            strokeCap = StrokeCap.Round,
            progress = { animatedProgress },
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
    modifier: Modifier = Modifier,
) {
    Tile(modifier = modifier) {
        val animatedRamProgress by animateFloatAsState(
            targetValue = (usedRamPercent ?: 0f) / 100f,
            animationSpec = tween(durationMillis = 600)
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)
        ) {
            Text(
                text = "RAM",
                color = text,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(height = 4.dp))
            Text(
                text = usedRamGb?.let { "%.1f GB / %.1f GB".format(it.first, it.second) } ?: "-",
                color = textWeak,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
            LinearProgressIndicator(
                color = animatedRamProgress.progressColor(),
                trackColor = backgroundTertiary,
                strokeCap = StrokeCap.Round,
                progress = { animatedRamProgress }
            )
            Spacer(modifier = Modifier.height(height = 4.dp))
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
) {
    val targetSweep = ((temperature - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f) * 180f
    val progressRatio = ((temperature - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)

    val animatedSweepAngle by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec = tween(durationMillis = 600)
    )

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
                color = backgroundTertiary,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 1.8f, radius * 1.8f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color = progressRatio.progressColor(),
                startAngle = 180f,
                sweepAngle = animatedSweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 1.8f, radius * 1.8f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun TemperatureTypeSelector(
    selected: TemperatureType,
    onSelect: (TemperatureType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val types = listOf(TemperatureType.Internal, TemperatureType.External)
    val shape = RoundedCornerShape(percent = 50)

    Row(
        modifier = modifier
            .background(
                color = backgroundTertiary,
                shape = shape
            )
            .padding(all = 4.dp)
    ) {

        types.forEach { type ->
            val isSelected = type == selected
            val animatedBackgroundColor by animateColorAsState(
                targetValue = if (isSelected) blue.copy(alpha = 0.99f) else Color.Transparent,
                animationSpec = tween(durationMillis = 600)
            )
            val textColor = if (isSelected) Color.White else textWeak
            val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

            Text(
                text = type.label,
                modifier = Modifier
                    .weight(weight = 1f)
                    .clip(shape)
                    .clickable { onSelect(type) }
                    .background(color = animatedBackgroundColor)
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                color = textColor,
                fontWeight = fontWeight,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private enum class TemperatureType(val label: String) {
    Internal("Internal"),
    External("External")
}

private fun ProcessInfo.getBadgeColorAndText() = when (this.stateDescription) {
    "sleeping" -> backgroundBadgeDisabled to textWeak
    else -> backgroundBadgeEnabled to text
}

fun Float.format(digits: Int): String =
    "%.${digits}f".format(this).replace('.', ',')

fun Float.progressColor(isIdle: Boolean = false): Color = when {
    this >= 0.75f -> if (isIdle) primary else tertiary
    this >= 0.5f -> secondary
    else -> if (isIdle) tertiary else primary
}


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
                usedRamGb = 1.2f to 3.7f,
                killingProcesses = emptySet()
            ),
            onEvent = {},
            effectFlow = emptyFlow()
        )
    }
}
