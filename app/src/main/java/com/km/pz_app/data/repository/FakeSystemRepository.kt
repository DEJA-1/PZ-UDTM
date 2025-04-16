package com.km.pz_app.data.repository

import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.CpuStats
import com.km.pz_app.domain.model.CpuUsage
import com.km.pz_app.domain.model.MemoryResponse
import com.km.pz_app.domain.model.ProcessInfo
import com.km.pz_app.domain.model.ProcessResponse
import com.km.pz_app.domain.repository.ISystemRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

class FakeSystemRepository @Inject constructor() : ISystemRepository {

    private var tick = 0
    private var previousUser = 100L
    private var previousKernel = 200L
    private var previousIdle = 3000L

    override suspend fun getCpuStatus(): CpuResponse {
        delay(300)
        tick++

        val userDelta = (1..5).random().toLong()
        val kernelDelta = (2..6).random().toLong()
        val idleDelta = (8..20).random().toLong()

        previousUser += userDelta
        previousKernel += kernelDelta
        previousIdle += idleDelta

        return CpuResponse(
            cpuTemperature = 40f + (tick % 5),
            cpuUsage = CpuUsage(
                full = CpuStats(
                    userNorm = previousUser,
                    userNice = 0,
                    kernel = previousKernel,
                    idle = previousIdle,
                    ioWait = 1,
                    irq = 0,
                    softIrq = 0
                ),
                cores = emptyList()
            )
        )
    }


    override suspend fun getMemoryStatus(): MemoryResponse {
        delay(300)

        val total = 3882924L
        val used = 500000L + tick * 1000
        val available = total - used
        val free = available / 2

        return MemoryResponse(
            total = total,
            available = available,
            free = free
        )
    }

    override suspend fun getProcesses(): ProcessResponse {
        delay(400)
        return ProcessResponse(
            processes = listOf(
                ProcessInfo(
                    pid = 1,
                    name = "systemd",
                    stateCode = "S",
                    stateDescription = "sleeping",
                    user = "0",
                    group = "0",
                    memoryRss = (88000 + tick * 100).toLong(),
                    memoryVirt = 150000,
                    swap = 0,
                    threads = 2,
                    uTime = (tick * 10).toLong()
                )
            )
        )
    }
}
