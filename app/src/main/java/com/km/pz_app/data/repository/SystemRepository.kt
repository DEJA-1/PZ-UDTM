package com.km.pz_app.data.repository

import com.km.pz_app.data.dataProvider.RaspberryAddressProvider
import com.km.pz_app.data.dataProvider.SystemStatusApi
import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.ExternalTemperatureResponse
import com.km.pz_app.domain.model.KillProcessRequest
import com.km.pz_app.domain.model.MemoryResponse
import com.km.pz_app.domain.model.ProcessResponse
import com.km.pz_app.domain.repository.ISystemRepository
import javax.inject.Inject

class SystemRepository @Inject constructor(
    private val api: SystemStatusApi,
    private val addressProvider: RaspberryAddressProvider,
) : ISystemRepository {
    override suspend fun getCpuStatus(): CpuResponse {
        val url = addressProvider
            .httpBaseUrl()
            .newBuilder()
            .addPathSegment("cpu")
            .build()
            .toString()

        return api.getCpuStatus(url = url)
    }

    override suspend fun getMemoryStatus(): MemoryResponse {
        val url = addressProvider
            .httpBaseUrl()
            .newBuilder()
            .addPathSegment("memory")
            .build()
            .toString()

        return api.getMemoryStatus(url = url)
    }

    override suspend fun getProcesses(): ProcessResponse {
        val url = addressProvider
            .httpBaseUrl()
            .newBuilder()
            .addPathSegment("processes")
            .build()
            .toString()

        return api.getProcesses(url = url)
    }

    override suspend fun getExternalTemperature(): ExternalTemperatureResponse {
        val url = addressProvider
            .httpBaseUrl()
            .newBuilder()
            .addPathSegment("ext_temp")
            .build()
            .toString()

        return api.getExternalTemperature(url = url)
    }

    override suspend fun killProcess(
        request: KillProcessRequest,
    ) {
        val url = addressProvider
            .httpBaseUrl()
            .newBuilder()
            .addPathSegment("control")
            .addPathSegment("process")
            .addPathSegment("kill")
            .build()
            .toString()

        api.killProcess(
            url = url,
            request = request,
        )
    }
}
