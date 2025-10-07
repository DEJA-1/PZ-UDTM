package com.km.pz_app.data.repository

import com.km.pz_app.domain.model.CpuResponse
import com.km.pz_app.domain.model.CpuStats
import com.km.pz_app.domain.model.CpuUsage
import com.km.pz_app.domain.model.ExternalTemperatureResponse
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
    private var totalRam = 3882924L
    private var currentUsedRam = 1500000L
    private val states = listOf("sleeping", "running", "zombie", "interrupt")
    private val names = listOf("systemd", "kworker", "irq/51", "node", "node", "node", "node")

    private val processes = (1..6).map {
        ProcessInfo(
            pid = it,
            name = names.random(),
            stateCode = "S",
            stateDescription = states.random(),
            user = "1000",
            group = "1000",
            memoryRss = (80_000..160_000).random().toLong(),
            memoryVirt = (200_000..300_000).random().toLong(),
            swap = 0,
            threads = (1..6).random(),
            uTime = (tick * (1..30).random()).toLong()
        )
    }.toMutableList()


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
            cpuTemperature = 20f + ((tick * 12) % 68),
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

    override suspend fun getExternalTemperature(): ExternalTemperatureResponse {
        tick++
        return ExternalTemperatureResponse(
            temperature = 20f + ((tick * 12) % 68)
        )
    }

    override suspend fun getMemoryStatus(): MemoryResponse {
        delay(300)

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
        return ProcessResponse(processes = processes)
    }

    override suspend fun killProcess(pid: Int) {
        processes.removeIf { it.pid == pid }
    }
}
