package com.km.pz_app.domain.repository

import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.ExternalTemperatureResponse
import com.km.pz_app.domain.model.KillProcessRequest
import com.km.pz_app.domain.model.MemoryResponse
import com.km.pz_app.domain.model.ProcessResponse

interface ISystemRepository {
    suspend fun getCpuStatus(): CpuResponse
    suspend fun getMemoryStatus(): MemoryResponse
    suspend fun getProcesses(): ProcessResponse
    suspend fun killProcess(request: KillProcessRequest): Unit
    suspend fun getExternalTemperature(): ExternalTemperatureResponse
}
