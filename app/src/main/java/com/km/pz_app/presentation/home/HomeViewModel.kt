package com.km.pz_app.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.km.pz_app.data.repository.FakeSystemRepository
import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.CpuStats
import com.km.pz_app.domain.repository.ISystemRepository
import com.km.pz_app.presentation.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

private val REFRESH_DATA_INTERVAL = 5.seconds

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FakeSystemRepository
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
    private var previousCpuStats: CpuStats? = null

    init {
        startDataRefreshLoop()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            else -> {}
        }
    }

    private fun startDataRefreshLoop() {
        viewModelScope.launch {
            while (true) {
                Log.d("test", "fetching data...")
                Log.d("test", "currentState: ${_state.value}")
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
            }.onSuccess {
                updateState {
                    copy(memory = Resource.Success(data = it))
                }
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

    private fun calculateAndUpdateCpu(newCpu: CpuResponse) {
        val current = newCpu.cpuUsage.full
        val calculatedPercent = previousCpuStats?.let { previous ->
            calculateCpuUsagePercentage(previous, current)
        }

        previousCpuStats = current

        updateState {
            copy(
                cpu = Resource.Success(data = newCpu),
                cpuPercentUsed = calculatedPercent
            )
        }
    }

    private fun calculateCpuUsagePercentage(
        previous: CpuStats,
        current: CpuStats
    ): Float {
        val prevActive =
            previous.userNorm + previous.userNice + previous.kernel + previous.ioWait + previous.irq + previous.softIrq
        val prevTotal = prevActive + previous.idle

        val currActive =
            current.userNorm + current.userNice + current.kernel + current.ioWait + current.irq + current.softIrq
        val currTotal = currActive + current.idle

        val totalDelta = currTotal - prevTotal
        val activeDelta = currActive - prevActive

        return if (totalDelta > 0) {
            (activeDelta.toFloat() / totalDelta.toFloat()) * 100f
        } else 0f
    }

    private fun updateState(update: HomeState.() -> HomeState) {
        _state.update { it.update() }
    }

}