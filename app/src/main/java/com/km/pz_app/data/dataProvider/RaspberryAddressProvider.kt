package com.km.pz_app.data.dataProvider

import com.km.pz_app.data.repository.SelectedRaspberryRepository
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RaspberryAddressProvider @Inject constructor(
    private val repository: SelectedRaspberryRepository
) {
    private val ipByIndex = listOf(
        "192.168.100.192",
        "192.168.100.192",
        "192.168.100.192",
    )

    suspend fun currentIp(): String {
        val index = repository.getSelectedIndex()
        return ipByIndex.getOrNull(index) ?: ipByIndex.first()
    }

    suspend fun httpBaseUrl(): HttpUrl =
        "http://${currentIp()}:3000/".toHttpUrl()

    suspend fun wsUrl(): String =
        "ws://${currentIp()}:3000/terminal/ws"
}