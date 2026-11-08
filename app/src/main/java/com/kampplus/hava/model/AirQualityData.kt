package com.kampplus.hava.model

data class AirQualityData(
    val aqi: Int? = null,
    val pm25: Double? = null,
    val pm10: Double? = null,
    val ozone: Double? = null,
    val uvIndex: Double? = null
)
