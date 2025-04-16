package com.km.pz_app.presentation.home

import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.MemoryResponse
import com.km.pz_app.domain.model.ProcessResponse
import com.km.pz_app.presentation.utils.Resource
import kotlinx.collections.immutable.persistentListOf

data class HomeState(
    val cpu: Resource<CpuResponse>,
    val memory: Resource<MemoryResponse>,
    val processes: Resource<ProcessResponse>,
) {
    private val resources = persistentListOf(cpu, memory, processes)
    val isLoading = resources.any { it is Resource.Loading }
    val isError = resources.any { it is Resource.Error }
}

sealed interface HomeEvent {

}

sealed interface HomeEffect {
}
