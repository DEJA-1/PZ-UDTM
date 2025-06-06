package com.km.pz_app.data.dataProvider

import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.ExternalTemperatureResponse
import com.km.pz_app.domain.model.KillProcessRequest
import com.km.pz_app.domain.model.MemoryResponse
import com.km.pz_app.domain.model.ProcessResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SystemStatusApi {
    @GET("/cpu")
    suspend fun getCpuStatus(): CpuResponse

    @GET("/memory")
    suspend fun getMemoryStatus(): MemoryResponse

    @GET("/processes")
    suspend fun getProcesses(): ProcessResponse

    @GET("/ext_temp")
    suspend fun getExternalTemperature(): ExternalTemperatureResponse

    @POST("/control/process/kill")
    suspend fun killProcess(@Body request: KillProcessRequest): Unit

}
