package com.km.pz_app.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.km.pz_app.data.dataProvider.RaspberryAddressProvider
import com.km.pz_app.data.repository.SelectedRaspberryRepository
import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.CpuStats
import com.km.pz_app.domain.model.KillProcessRequest
import com.km.pz_app.domain.model.MemoryResponse
import com.km.pz_app.domain.repository.ISystemRepository
import com.km.pz_app.presentation.nav.Destination
import com.km.pz_app.presentation.nav.navigator.INavigator
import com.km.pz_app.presentation.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

private val REFRESH_DATA_INTERVAL = 6.seconds

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ISystemRepository,
    private val navigator: INavigator,
    private val raspberryRepository: SelectedRaspberryRepository,
    private val raspberryAddressProvider: RaspberryAddressProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(
        HomeState(
            cpu = Resource.Loading,
            memory = Resource.Loading,
            processes = Resource.Loading,
            killingProcesses = emptySet(),
            raspberrysCount = raspberryAddressProvider.getCount(),
            newIpInputValue = "",
            ipError = false,
        )
    )
    val state = _state.asStateFlow()

    private var initialInvoke = true
    private var refreshJob: Job? = null

    private val effectChannel: Channel<HomeEffect> = Channel(Channel.CONFLATED)
    val effectFlow = effectChannel.receiveAsFlow()

    init {
        startDataRefreshLoop()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.RemoteTerminalClick -> navigator.pushNavigationEvent(Destination.RemoteTerminal)
            is HomeEvent.ProcessKillClick -> handleProcessKill(pid = event.pid)
            is HomeEvent.RaspberryIndexChange -> handleRaspberryIndexChange(index = event.index)
            is HomeEvent.AddIpClick -> handleAddIpClick(ip = event.ip)
            is HomeEvent.InputValueChange -> handleInputValueChange(newValue = event.newValue)
        }
    }

    private fun startDataRefreshLoop() {
        refreshJob?.cancel()

        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            initialInvoke = true
            delay(200)
            while (isActive) {
                fetchData(showLoading = initialInvoke)
                delay(duration = if (initialInvoke) 0.seconds else REFRESH_DATA_INTERVAL)
                initialInvoke = false
            }
        }
    }

    private suspend fun fetchData(showLoading: Boolean) = coroutineScope {
        launch { fetchCpuData(showLoading = showLoading) }
        launch { fetchMemoryData(showLoading = showLoading) }
        launch { fetchProcessesData(showLoading = showLoading) }
        launch { fetchExternalTemperature(showLoading = showLoading) }
    }

    private suspend fun fetchCpuData(showLoading: Boolean) {
        if (showLoading) {
            updateState {
                copy(cpu = Resource.Loading)
            }
        }

        runCatching {
            repository.getCpuStatus()
        }.onSuccess { newCpu ->
            calculateAndUpdateCpu(newCpu = newCpu)
        }.onFailure {
            updateState {
                copy(cpu = Resource.Error(message = "Error: ${it.message}"))
            }
            Log.w("Error", it.message.toString())
        }
    }

    private suspend fun fetchMemoryData(showLoading: Boolean) {
        if (showLoading) {
            updateState {
                copy(memory = Resource.Loading)
            }
        }

        runCatching {
            repository.getMemoryStatus()
        }.onSuccess { memoryResponse ->
            val (percent, gbPair) = withContext(Dispatchers.Default) {
                mapMemoryState(memory = Resource.Success(memoryResponse))
            }
            updateState {
                copy(
                    memory = Resource.Success(data = memoryResponse),
                    usedRamPercent = percent,
                    usedRamGb = gbPair
                )
            }
        }.onFailure {
            updateState {
                copy(memory = Resource.Error(message = "Error: ${it.message}"))
            }
            Log.w("Error", it.message.toString())
        }
    }

    private suspend fun fetchProcessesData(showLoading: Boolean) {
        if (showLoading) {
            updateState {
                copy(processes = Resource.Loading)
            }
        }

        runCatching {
            repository.getProcesses()
        }.onSuccess {
            updateState {
                copy(processes = Resource.Success(data = it))
            }
        }.onFailure {
            updateState {
                copy(processes = Resource.Error(message = "Error: ${it.message}"))
            }
            Log.w("Error", it.message.toString())
        }
    }

    private suspend fun fetchExternalTemperature(showLoading: Boolean) {
        if (showLoading) {
            updateState {
                copy(processes = Resource.Loading)
            }
        }

        runCatching {
            repository.getExternalTemperature()
        }.onSuccess {
            updateState {
                copy(
                    externalTemperatureResource = Resource.Success(data = it),
                    externalTemperature = it.temperature,
                )
            }
        }.onFailure {
            Log.w("Error", it.message.toString())
        }
    }

    private suspend fun calculateAndUpdateCpu(newCpu: CpuResponse) {
        val (percent, temp) = withContext(Dispatchers.Default) {
            mapCpuState(cpu = newCpu)
        }

        updateState {
            copy(
                cpu = Resource.Success(data = newCpu),
                cpuPercentUsed = percent,
                cpuTemperature = temp
            )
        }
    }

    private fun handleProcessKill(pid: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.killProcess(request = KillProcessRequest(pid = pid))
            }.onSuccess {
                updateState { copy(killingProcesses = killingProcesses + pid) }
                pushEffect(HomeEffect.KillProcessSuccess)
            }.onFailure {
                pushEffect(HomeEffect.KillProcessFailure)
                Log.w("Error", it.message.toString())
            }
        }
    }

    private fun handleRaspberryIndexChange(index: Int) {
        viewModelScope.launch {
            raspberryRepository.setSelectedIndex(index = index)
            startDataRefreshLoop()
        }
    }

    private fun handleAddIpClick(ip: String) {
        raspberryAddressProvider.addIp(ip = ip)
        _state.update {
            it.copy(
                raspberrysCount = raspberryAddressProvider.getCount(),
                ipError = false,
                newIpInputValue = ""
            )
        }
    }

    private fun handleInputValueChange(newValue: String) {
        val cleaned = newValue.filter { it.isDigit() || it == '.' }

        if (!ipPartialRegex.matches(cleaned)) {
            return
        }

        val isValid = isFullIpValid(value = cleaned)

        _state.update {
            it.copy(
                newIpInputValue = cleaned,
                ipError = cleaned.isNotEmpty() && !isValid
            )
        }
    }

    private val ipPartialRegex = Regex("""^(\d{1,3}(\.\d{0,3}){0,3})?$""")

    private fun isFullIpValid(value: String): Boolean {
        val parts = value.split(".")
        if (parts.size != 4) return false

        return parts.all { part ->
            if (part.isEmpty() || part.length > 3) return false
            val number = part.toIntOrNull() ?: return false
            number in 0..255
        }
    }

    private fun mapCpuState(cpu: CpuResponse): Pair<Float, Float> {
        val cpuUsagePercent = calculateCpuUsageFromSnapshot(stats = cpu.cpuUsage.full)
        val temp = cpu.cpuTemperature
        return cpuUsagePercent to temp
    }

    private fun mapMemoryState(memory: Resource<MemoryResponse>): Pair<Float?, Pair<Float, Float>?> {
        val percent = (memory as? Resource.Success)?.data?.let {
            100f * (it.total - it.available) / it.total
        }
        val gbPair = (memory as? Resource.Success)?.data?.let {
            val used = (it.total - it.available).toFloat() / 1024 / 1024
            val total = it.total.toFloat() / 1024 / 1024
            used to total
        }
        return percent to gbPair
    }

    private fun calculateCpuUsageFromSnapshot(stats: CpuStats): Float {
        val total = stats.userNorm + stats.userNice + stats.kernel + stats.idle +
                stats.ioWait + stats.irq + stats.softIrq

        return if (total > 0) {
            ((total - stats.idle).toFloat() / total.toFloat()) * 100f
        } else {
            0f
        }
    }

    private fun updateState(update: HomeState.() -> HomeState) {
        _state.update { it.update() }
    }

    private suspend fun pushEffect(effect: HomeEffect) {
        withContext(Dispatchers.Main.immediate) {
            effectChannel.trySend(effect)
        }
    }
}