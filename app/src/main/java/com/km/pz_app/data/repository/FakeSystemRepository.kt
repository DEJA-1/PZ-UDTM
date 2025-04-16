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
import kotlin.random.Random

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


    private var totalRam = 3882924L // 3.7 GB
    private var currentUsedRam = 1500000L // Start: ~1.5 GB

    override suspend fun getMemoryStatus(): MemoryResponse {
        delay(300)

        // Losowy przyrost/zmniejszenie zużycia: od -100 do +200 MB
        val usageDelta = (listOf(-200_000, -100_000, 0, 100_000, 200_000)).random()
        currentUsedRam = (currentUsedRam + usageDelta).coerceIn(500_000L, totalRam - 200_000L)

        val available = totalRam - currentUsedRam
        val free = (available * Random.nextDouble(0.3, 0.6)).toLong()

        return MemoryResponse(
            total = totalRam,
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
