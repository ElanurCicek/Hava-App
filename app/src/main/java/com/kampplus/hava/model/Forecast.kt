package com.kampplus.hava.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Bir şehrin anlık durumu + önümüzdeki saatler ve günler.
 */
data class Forecast(
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val utcOffsetSeconds: Int? = null
)

data class CurrentWeather(
    val temperatureC: Double,
    val weatherCode: Int,
    val observedAt: LocalDateTime,
    val apparentTemperatureC: Double? = null,
    val humidityPercent: Int? = null,
    val windSpeedKmh: Double? = null,
    val windDirectionDegrees: Int? = null,
    val surfacePressureHpa: Double? = null
)

data class HourlyForecast(
    val time: LocalDateTime,
    val temperatureC: Double,
    val weatherCode: Int,
    val precipitationProbability: Int? = null
)

data class DailyForecast(
    val date: LocalDate,
    val minTemperatureC: Double?,
    val maxTemperatureC: Double?,
    val weatherCode: Int?,
    val precipitationProbability: Int? = null,
    val sunrise: LocalDateTime? = null,
    val sunset: LocalDateTime? = null
)
