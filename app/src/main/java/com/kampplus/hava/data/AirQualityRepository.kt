package com.kampplus.hava.data

import com.kampplus.hava.model.AirQualityData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AirQualityRepository {
    suspend fun getAirQuality(latitude: Double, longitude: Double): AirQualityData
}

class OpenMeteoAirQualityRepository(
    private val api: AirQualityApi = ApiClient.airQualityApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AirQualityRepository {
    override suspend fun getAirQuality(latitude: Double, longitude: Double): AirQualityData = withContext(ioDispatcher) {
        try {
            val response = api.getAirQuality(latitude, longitude)
            val curr = response.current
            AirQualityData(
                aqi = curr?.europeanAqi,
                pm25 = curr?.pm25,
                pm10 = curr?.pm10,
                ozone = curr?.ozone,
                uvIndex = curr?.uvIndex
            )
        } catch (_: Exception) {
            AirQualityData()
        }
    }
}
