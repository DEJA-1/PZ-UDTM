package com.km.pz_app.domain.model

import com.google.gson.annotations.SerializedName

data class CpuResponse(
    @SerializedName("cpu_temperature") val cpuTemperature: Float,
    @SerializedName("cpu_usage") val cpuUsage: CpuUsage
)

data class CpuUsage(
    @SerializedName("full") val full: CpuStats,
    @SerializedName("cores") val cores: List<CoreStats>
)

data class CpuStats(
    @SerializedName("user_norm") val userNorm: Long,
    @SerializedName("user_nice") val userNice: Long,
    @SerializedName("kernel") val kernel: Long,
    @SerializedName("idle") val idle: Long,
    @SerializedName("iowait") val ioWait: Long,
    @SerializedName("irq") val irq: Long,
    @SerializedName("soft_irq") val softIrq: Long
)

data class CoreStats(
    @SerializedName("core_id") val coreId: Int,
    @SerializedName("stats") val stats: CpuStats
)
