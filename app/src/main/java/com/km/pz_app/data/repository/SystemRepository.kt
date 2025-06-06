package com.km.pz_app.data.repository

import com.km.pz_app.data.dataProvider.SystemStatusApi
import com.km.pz_app.domain.model.ExternalTemperatureResponse
import com.km.pz_app.domain.model.KillProcessRequest
import com.km.pz_app.domain.repository.ISystemRepository
import javax.inject.Inject

class SystemRepository @Inject constructor(
    private val api: SystemStatusApi
) : ISystemRepository {
    override suspend fun getCpuStatus() = api.getCpuStatus()
    override suspend fun getMemoryStatus() = api.getMemoryStatus()
    override suspend fun getProcesses() = api.getProcesses()
    override suspend fun killProcess(pid: Int) = api.killProcess(
        request = KillProcessRequest(pid = pid)
    )
    override suspend fun getExternalTemperature(): ExternalTemperatureResponse =
        api.getExternalTemperature()
}
