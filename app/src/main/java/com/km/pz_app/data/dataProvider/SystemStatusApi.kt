package com.km.pz_app.data.dataProvider

import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.ExternalTemperatureResponse
import com.km.pz_app.domain.model.KillProcessRequest
import com.km.pz_app.domain.model.MemoryResponse
import com.km.pz_app.domain.model.ProcessResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface SystemStatusApi {

    @GET
    suspend fun getCpuStatus(
        @Url url: String,
    ): CpuResponse

    @GET
    suspend fun getMemoryStatus(
        @Url url: String,
    ): MemoryResponse

    @GET
    suspend fun getProcesses(
        @Url url: String,
    ): ProcessResponse

    @GET
    suspend fun getExternalTemperature(
        @Url url: String,
    ): ExternalTemperatureResponse

    @POST
    suspend fun killProcess(
        @Url url: String,
        @Body request: KillProcessRequest,
    )
}
