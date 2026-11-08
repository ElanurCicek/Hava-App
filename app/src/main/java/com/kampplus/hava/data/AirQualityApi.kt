package com.kampplus.hava.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface AirQualityApi {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "european_aqi,pm10,pm2_5,ozone,uv_index",
        @Query("timezone") timezone: String = "auto"
    ): AirQualityResponse
}

data class AirQualityResponse(
    @SerializedName("current")
    val current: CurrentAirQualityResponse? = null
)

data class CurrentAirQualityResponse(
    @SerializedName("european_aqi")
    val europeanAqi: Int? = null,
    @SerializedName("pm10")
    val pm10: Double? = null,
    @SerializedName("pm2_5")
    val pm25: Double? = null,
    @SerializedName("ozone")
    val ozone: Double? = null,
    @SerializedName("uv_index")
    val uvIndex: Double? = null
)
