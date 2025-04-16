package com.km.pz_app.data.dataProvider

import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.MemoryResponse
import com.km.pz_app.domain.model.ProcessResponse
import retrofit2.http.GET

interface SystemStatusApi {
    @GET("/cpu")
    suspend fun getCpuStatus(): CpuResponse

    @GET("/memory")
    suspend fun getMemoryStatus(): MemoryResponse

    @GET("/processes")
    suspend fun getProcesses(): ProcessResponse
}
