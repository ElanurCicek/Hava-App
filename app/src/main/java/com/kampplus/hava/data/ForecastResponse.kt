package com.kampplus.hava.data

import com.google.gson.annotations.SerializedName
import com.kampplus.hava.model.CurrentWeather
import com.kampplus.hava.model.DailyForecast
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.model.HourlyForecast
import java.time.LocalDate
import java.time.LocalDateTime

data class ForecastResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerializedName("utc_offset_seconds")
    val utcOffsetSeconds: Int? = null,
    val current: CurrentResponse? = null,
    val hourly: HourlyResponse? = null,
    val daily: DailyResponse? = null
)

data class CurrentResponse(
    val time: String,
    @SerializedName("temperature_2m")
    val temperature: Double,
    @SerializedName("weather_code")
    val weatherCode: Int,
    @SerializedName("apparent_temperature")
    val apparentTemperature: Double? = null,
    @SerializedName("relative_humidity_2m")
    val relativeHumidity: Int? = null,
    @SerializedName("wind_speed_10m")
    val windSpeed: Double? = null,
    @SerializedName("wind_direction_10m")
    val windDirection: Int? = null,
    @SerializedName("surface_pressure")
    val surfacePressure: Double? = null
)

data class HourlyResponse(
    val time: List<String>? = null,
    @SerializedName("temperature_2m")
    val temperature: List<Double?>? = null,
    @SerializedName("weather_code")
    val weatherCode: List<Int?>? = null,
    @SerializedName("precipitation_probability")
    val precipitationProbability: List<Int?>? = null
)

data class DailyResponse(
    val time: List<String>? = null,
    @SerializedName("weather_code")
    val weatherCode: List<Int?>? = null,
    @SerializedName("temperature_2m_max")
    val temperatureMax: List<Double?>? = null,
    @SerializedName("temperature_2m_min")
    val temperatureMin: List<Double?>? = null,
    @SerializedName("precipitation_probability_max")
    val precipitationProbabilityMax: List<Int?>? = null,
    @SerializedName("sunrise")
    val sunrise: List<String?>? = null,
    @SerializedName("sunset")
    val sunset: List<String?>? = null
)

fun ForecastResponse.toForecast(): Forecast {
    val currentResponse = current
        ?: throw IllegalStateException("Forecast response has no current block")
    return Forecast(
        current = currentResponse.toCurrentWeather(),
        hourly = hourly.toHourlyForecasts(),
        daily = daily.toDailyForecasts(),
        utcOffsetSeconds = utcOffsetSeconds
    )
}

private fun CurrentResponse.toCurrentWeather() = CurrentWeather(
    temperatureC = temperature,
    weatherCode = weatherCode,
    observedAt = LocalDateTime.parse(time),
    apparentTemperatureC = apparentTemperature,
    humidityPercent = relativeHumidity,
    windSpeedKmh = windSpeed,
    windDirectionDegrees = windDirection,
    surfacePressureHpa = surfacePressure
)

private fun HourlyResponse?.toHourlyForecasts(): List<HourlyForecast> {
    val times = this?.time ?: return emptyList()
    return times.indices.mapNotNull { index ->
        val temperature = temperature?.getOrNull(index) ?: return@mapNotNull null
        val code = weatherCode?.getOrNull(index) ?: return@mapNotNull null
        HourlyForecast(
            time = LocalDateTime.parse(times[index]),
            temperatureC = temperature,
            weatherCode = code,
            precipitationProbability = precipitationProbability?.getOrNull(index)
        )
    }
}

private fun DailyResponse?.toDailyForecasts(): List<DailyForecast> {
    val times = this?.time ?: return emptyList()
    return times.indices.mapNotNull { index ->
        val dateStr = times.getOrNull(index) ?: return@mapNotNull null
        val max = temperatureMax?.getOrNull(index) ?: return@mapNotNull null
        val min = temperatureMin?.getOrNull(index) ?: return@mapNotNull null
        val code = weatherCode?.getOrNull(index) ?: return@mapNotNull null
        val sunriseStr = sunrise?.getOrNull(index)
        val sunsetStr = sunset?.getOrNull(index)
        DailyForecast(
            date = LocalDate.parse(dateStr),
            minTemperatureC = min,
            maxTemperatureC = max,
            weatherCode = code,
            precipitationProbability = precipitationProbabilityMax?.getOrNull(index),
            sunrise = sunriseStr?.let { try { LocalDateTime.parse(it) } catch (_: Exception) { null } },
            sunset = sunsetStr?.let { try { LocalDateTime.parse(it) } catch (_: Exception) { null } }
        )
    }
}
