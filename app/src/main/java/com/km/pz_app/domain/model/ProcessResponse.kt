package com.km.pz_app.domain.model

import com.google.gson.annotations.SerializedName

data class ProcessResponse(
    @SerializedName("processes") val processes: List<ProcessInfo>
)

data class ProcessInfo(
    @SerializedName("pid") val pid: Int,
    @SerializedName("name") val name: String,
    @SerializedName("state_code") val stateCode: String,
    @SerializedName("state_description") val stateDescription: String,
    @SerializedName("user") val user: String,
    @SerializedName("group") val group: String,
    @SerializedName("memory_rss") val memoryRss: Long,
    @SerializedName("memory_virt") val memoryVirt: Long,
    @SerializedName("swap") val swap: Long,
    @SerializedName("threads") val threads: Int,
    @SerializedName("utime") val uTime: Long
)
