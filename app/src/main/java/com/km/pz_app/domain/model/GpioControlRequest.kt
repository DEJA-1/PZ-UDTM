package com.km.pz_app.domain.model

data class GpioControlRequest(
    val gpio_num: Int,
    val gpio_val: Int
)