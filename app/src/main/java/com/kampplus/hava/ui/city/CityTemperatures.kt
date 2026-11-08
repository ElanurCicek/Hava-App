package com.kampplus.hava.ui.city

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.kampplus.hava.model.City
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.roundToInt

data class CityBatchCurrentResponse(
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("current") val current: CityBatchCurrent? = null
)

data class CityBatchCurrent(
    @SerializedName("temperature_2m") val temperature: Double? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null
)

interface CityTemperaturesApi {
    @GET("v1/forecast")
    suspend fun getBatchCurrentWeather(
        @Query("latitude") latitudes: String,
        @Query("longitude") longitudes: String,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("timezone") timezone: String = "auto"
    ): JsonElement

    @GET("v1/forecast")
    suspend fun getSingleCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("timezone") timezone: String = "auto"
    ): CityBatchCurrentResponse
}

object CityTemperaturesRepository {
    private val api: CityTemperaturesApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CityTemperaturesApi::class.java)
    }

    private val gson = Gson()

    /**
     * displayCities içindeki şehirlerin ilk 20 tanesi için tek bir Open-Meteo isteği atar.
     * Dönen sonuçları Map<String, Int> (şehir adı -> yuvarlanmış sıcaklık) olarak döndürür.
     */
    suspend fun fetchBatchTemperatures(cities: List<City>): Map<String, Int> = withContext(Dispatchers.IO) {
        if (cities.isEmpty()) return@withContext emptyMap()
        val targetCities = cities.take(20)
        try {
            val lats = targetCities.joinToString(",") { it.latitude.toString() }
            val lons = targetCities.joinToString(",") { it.longitude.toString() }

            val jsonElement = api.getBatchCurrentWeather(latitudes = lats, longitudes = lons)
            val responseList = mutableListOf<CityBatchCurrentResponse>()

            if (jsonElement.isJsonArray) {
                val array = gson.fromJson(jsonElement, Array<CityBatchCurrentResponse>::class.java)
                responseList.addAll(array)
            } else if (jsonElement.isJsonObject) {
                val obj = gson.fromJson(jsonElement, CityBatchCurrentResponse::class.java)
                responseList.add(obj)
            }

            val resultMap = mutableMapOf<String, Int>()
            for (i in targetCities.indices) {
                val responseItem = responseList.getOrNull(i)
                val temp = responseItem?.current?.temperature
                if (temp != null) {
                    resultMap[targetCities[i].name] = temp.roundToInt()
                }
            }
            resultMap
        } catch (e: Exception) {
            Log.e("CityTemperatures", "Batch sıcaklık verileri alınamadı", e)
            emptyMap()
        }
    }

    /**
     * Seçili şehir/konum için tek bir sıcaklık ve weatherCode çağrısı yapar.
     */
    suspend fun fetchSingleWeather(latitude: Double, longitude: Double): Pair<Int, Int>? = withContext(Dispatchers.IO) {
        try {
            val response = api.getSingleCurrentWeather(latitude = latitude, longitude = longitude)
            val temp = response.current?.temperature
            val code = response.current?.weatherCode
            if (temp != null) {
                Pair(temp.roundToInt(), code ?: 0)
            } else null
        } catch (e: Exception) {
            Log.e("CityTemperatures", "Tekil sıcaklık alınamadı", e)
            null
        }
    }
}
