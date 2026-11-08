package com.kampplus.hava.ui.forecast

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kampplus.hava.data.AirQualityRepository
import com.kampplus.hava.data.FavoritesRepository
import com.kampplus.hava.data.OpenMeteoAirQualityRepository
import com.kampplus.hava.data.OpenMeteoWeatherRepository
import com.kampplus.hava.data.WeatherRepository
import com.kampplus.hava.model.AirQualityData
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.ui.LoadState
import java.io.IOException
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ForecastViewModel(
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val repository: WeatherRepository = OpenMeteoWeatherRepository(),
    private val airQualityRepository: AirQualityRepository = OpenMeteoAirQualityRepository()
) : ViewModel() {

    data class FavoriteCity(
        val name: String,
        val subtitle: String,
        val latitude: Double,
        val longitude: Double
    )

    var favoriteCities by mutableStateOf<List<FavoriteCity>>(FavoritesRepository.DEFAULT_FAVORITES)
        private set

    private var favoritesRepository: FavoritesRepository? = null

    fun initFavorites(context: Context) {
        if (favoritesRepository == null) {
            val repo = FavoritesRepository(context.applicationContext)
            favoritesRepository = repo
            favoriteCities = repo.getFavorites()
        }
    }

    fun isCurrentCityFavorite(): Boolean {
        val currentLat = latitudeStr.toDoubleOrNull() ?: 41.0082
        val currentLon = longitudeStr.toDoubleOrNull() ?: 28.9784
        return favoriteCities.any {
            it.name.equals(cityName, ignoreCase = true) ||
            (abs(it.latitude - currentLat) < 0.01 && abs(it.longitude - currentLon) < 0.01)
        }
    }

    fun toggleFavorite(context: Context) {
        initFavorites(context)
        val currentLat = latitudeStr.toDoubleOrNull() ?: 41.0082
        val currentLon = longitudeStr.toDoubleOrNull() ?: 28.9784
        val existing = favoriteCities.firstOrNull {
            it.name.equals(cityName, ignoreCase = true) ||
            (abs(it.latitude - currentLat) < 0.01 && abs(it.longitude - currentLon) < 0.01)
        }

        if (existing != null) {
            removeFavorite(context, existing)
        } else {
            addFavorite(context, FavoriteCity(cityName, subtitle, currentLat, currentLon))
        }
    }

    fun addFavorite(context: Context, city: FavoriteCity) {
        initFavorites(context)
        if (favoriteCities.none { it.name.equals(city.name, ignoreCase = true) }) {
            val updated = favoriteCities + city
            favoriteCities = updated
            favoritesRepository?.saveFavorites(updated)
        }
    }

    fun removeFavorite(context: Context, city: FavoriteCity) {
        initFavorites(context)
        val updated = favoriteCities.filterNot { it.name.equals(city.name, ignoreCase = true) }
        favoriteCities = updated
        favoritesRepository?.saveFavorites(updated)
    }

    var cityName by mutableStateOf(savedStateHandle[ARG_NAME] ?: savedStateHandle["name"] ?: savedStateHandle["cityName"] ?: "İstanbul")
        private set

    var subtitle by mutableStateOf(savedStateHandle[ARG_SUBTITLE] ?: savedStateHandle["subtitle"] ?: "İstanbul, Türkiye")
        private set

    var airQualityData by mutableStateOf<AirQualityData?>(null)
        private set

    val latitudeStr: String = savedStateHandle[ARG_LATITUDE] ?: savedStateHandle["latitude"] ?: "41.0082"
    val longitudeStr: String = savedStateHandle[ARG_LONGITUDE] ?: savedStateHandle["longitude"] ?: "28.9784"

    private val _uiState = MutableStateFlow<LoadState<Forecast>>(LoadState.Loading)
    val uiState: StateFlow<LoadState<Forecast>> = _uiState.asStateFlow()

    init {
        loadWeather()
    }

    private fun loadWeather() {
        val lat = latitudeStr.toDoubleOrNull() ?: 41.0082
        val lon = longitudeStr.toDoubleOrNull() ?: 28.9784
        loadWeatherForCoordinates(lat, lon)
    }

    fun onRetry() {
        loadWeather()
    }

    fun loadWeatherForLocation(lat: Double, lon: Double, locationName: String? = null) {
        if (!locationName.isNullOrEmpty()) {
            cityName = locationName
            subtitle = "$locationName, Türkiye"
        }
        loadWeatherForCoordinates(lat, lon)
    }

    fun selectFavoriteCity(city: FavoriteCity) {
        loadWeatherForLocation(city.latitude, city.longitude, city.name)
    }

    private fun loadWeatherForCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = LoadState.Loading
            _uiState.value = try {
                val forecast = repository.getForecast(lat, lon)
                try {
                    airQualityData = airQualityRepository.getAirQuality(lat, lon)
                } catch (_: Exception) {
                    airQualityData = AirQualityData()
                }
                LoadState.Success(forecast)
            } catch (e: IOException) {
                Log.e(TAG, "Hava durumu alınamadı: bağlantı hatası", e)
                LoadState.Error("İnternet bağlantını kontrol et.")
            } catch (e: HttpException) {
                Log.e(TAG, "Hava durumu alınamadı: HTTP ${e.code()}", e)
                LoadState.Error("Sunucu şu an cevap vermiyor.")
            } catch (e: Exception) {
                Log.e(TAG, "Hava durumu alınamadı", e)
                LoadState.Error(e.message ?: "Bir şeyler ters gitti. Tekrar dene.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentGpsLocation(context: Context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null && !isEmulatorDefaultLocation(location.latitude, location.longitude)) {
                    val locationName = getCityNameFromCoordinates(context, location.latitude, location.longitude)
                    if (locationName != "Mevcut Konum" && locationName.isNotEmpty()) {
                        loadWeatherForLocation(location.latitude, location.longitude, locationName)
                    } else {
                        loadWeather()
                    }
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null && !isEmulatorDefaultLocation(lastLoc.latitude, lastLoc.longitude)) {
                            val locationName = getCityNameFromCoordinates(context, lastLoc.latitude, lastLoc.longitude)
                            loadWeatherForLocation(lastLoc.latitude, lastLoc.longitude, locationName)
                        } else {
                            loadWeather()
                        }
                    }.addOnFailureListener {
                        loadWeather()
                    }
                }
            }
            .addOnFailureListener {
                loadWeather()
            }
    }

    private fun isEmulatorDefaultLocation(lat: Double, lon: Double): Boolean {
        return lat in 37.35..37.48 && lon in -122.15..-122.00
    }

    private fun getCityNameFromCoordinates(context: Context, lat: Double, lon: Double): String {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.forLanguageTag("tr-TR"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val cityNameResult = address.locality ?: address.subAdminArea ?: address.adminArea ?: address.featureName
                    if (!cityNameResult.isNullOrEmpty()) {
                        return cityNameResult
                    }
                }
            }
        } catch (_: Exception) {}
        return "Mevcut Konum"
    }

    companion object {
        private const val TAG = "ForecastViewModel"
        const val ARG_NAME = "name"
        const val ARG_SUBTITLE = "subtitle"
        const val ARG_LATITUDE = "latitude"
        const val ARG_LONGITUDE = "longitude"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val savedStateHandle = extras.createSavedStateHandle()
                val repository = OpenMeteoWeatherRepository()
                val airQualityRepository = OpenMeteoAirQualityRepository()
                return ForecastViewModel(savedStateHandle, repository, airQualityRepository) as T
            }
        }
    }
}
