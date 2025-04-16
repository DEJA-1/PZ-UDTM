package com.km.pz_app.presentation.home

import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.MemoryResponse
import com.km.pz_app.domain.model.ProcessResponse
import com.km.pz_app.presentation.utils.Resource
import kotlinx.collections.immutable.persistentListOf

data class HomeState(
    private val cpu: Resource<CpuResponse>,
    private val memory: Resource<MemoryResponse>,
    private val processes: Resource<ProcessResponse>,
    val cpuPercentUsed: Float? = null,
    val cpuTemperature: Float? = null,
    val usedRamPercent: Float? = null,
    val usedRamGb: Pair<Float, Float>? = null
) {
    private val resources = persistentListOf(cpu, memory, processes)
    val isLoading = resources.any { it is Resource.Loading }
    val isError = resources.any { it is Resource.Error }
}

sealed interface HomeEvent {

}

sealed interface HomeEffect {
}
