package com.km.pz_app.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.CpuStats
import com.km.pz_app.domain.model.MemoryResponse
import com.km.pz_app.domain.repository.ISystemRepository
import com.km.pz_app.presentation.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

private val REFRESH_DATA_INTERVAL = 6.seconds

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ISystemRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        HomeState(
            cpu = Resource.Loading,
            memory = Resource.Loading,
            processes = Resource.Loading
        )
    )
    val state = _state.asStateFlow()
    private var initialInvoke = true
    private val effectChannel: Channel<HomeEffect> = Channel(Channel.CONFLATED)
    val effectFlow = effectChannel.receiveAsFlow()

    init {
        startDataRefreshLoop()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.ProcessKillClick -> handleProcessKill(pid = event.pid)
        }
    }

    private fun startDataRefreshLoop() {
        viewModelScope.launch {
            while (true) {
                fetchData(showLoading = initialInvoke)
                delay(duration = if (initialInvoke) 0.seconds else REFRESH_DATA_INTERVAL)
                initialInvoke = false
            }
        }
    }

    private fun fetchData(showLoading: Boolean) {
        fetchCpuData(showLoading = showLoading)
        fetchMemoryData(showLoading = showLoading)
        fetchProcessesData(showLoading = showLoading)
        fetchExternalTemperature(showLoading = showLoading)
    }

    private fun fetchCpuData(showLoading: Boolean) {
        if (showLoading) {
            updateState {
                copy(cpu = Resource.Loading)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.getCpuStatus()
            }.onSuccess { newCpu ->
                calculateAndUpdateCpu(newCpu)
            }.onFailure {
                updateState {
                    copy(cpu = Resource.Error(message = "Error: ${it.message}"))
                }
                Log.w("Error", it.message.toString())
            }
        }
    }

    private fun fetchMemoryData(showLoading: Boolean) {
        if (showLoading) {
            updateState {
                copy(memory = Resource.Loading)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.getMemoryStatus()
            }.onSuccess { memoryResponse ->
                val (percent, gbPair) = withContext(Dispatchers.Default) {
                    mapMemoryState(Resource.Success(memoryResponse))
                }
                updateState {
                    copy(
                        memory = Resource.Success(data = memoryResponse),
                        usedRamPercent = percent,
                        usedRamGb = gbPair
                    )
                }
                Log.d("test", memoryResponse.toString())
            }.onFailure {
                updateState {
                    copy(memory = Resource.Error(message = "Error: ${it.message}"))
                }
                Log.w("Error", it.message.toString())
            }
        }
    }

    private fun fetchProcessesData(showLoading: Boolean) {
        if (showLoading) {
            updateState {
                copy(processes = Resource.Loading)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
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
    }

    private fun fetchExternalTemperature(showLoading: Boolean) {
        if (showLoading) {
            updateState {
                copy(processes = Resource.Loading)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
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
                updateState {
                    copy(externalTemperatureResource = Resource.Error(message = "Error: ${it.message}"))
                }
                Log.w("Error", it.message.toString())
            }
        }
    }

    private suspend fun calculateAndUpdateCpu(newCpu: CpuResponse) {
        val (percent, temp) = withContext(Dispatchers.Default) {
            mapCpuState(newCpu)
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
                repository.killProcess(pid = pid)
            }.onSuccess {
                pushEffect(HomeEffect.KillProcessSuccess)
            }.onFailure {
                pushEffect(HomeEffect.KillProcessFailure)
                Log.w("Error", it.message.toString())
            }
        }
    }

    private fun mapCpuState(cpu: CpuResponse): Pair<Float, Float> {
        val cpuUsagePercent = calculateCpuUsageFromSnapshot(cpu.cpuUsage.full)
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