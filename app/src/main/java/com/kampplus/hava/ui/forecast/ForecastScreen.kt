package com.kampplus.hava.ui.forecast

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.hava.R
import com.kampplus.hava.model.AirQualityData
import com.kampplus.hava.model.CurrentWeather
import com.kampplus.hava.model.DailyForecast
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.model.HourlyForecast
import com.kampplus.hava.model.WeatherCondition
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.component.ErrorView
import com.kampplus.hava.ui.theme.HavaTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Tahmin ekranının bağlayıcı katmanı; ViewModel'i edinir ve state'i aktarır.
 */
@Composable
fun ForecastRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToCity: () -> Unit = onBack,
    viewModel: ForecastViewModel = viewModel(factory = ForecastViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.any { it }
        if (isGranted) {
            viewModel.fetchCurrentGpsLocation(context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initFavorites(context)
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val isDefaultLocation = viewModel.latitudeStr == "41.0082" && viewModel.longitudeStr == "28.9784"
        if (isDefaultLocation) {
            if (fineGranted || coarseGranted) {
                viewModel.fetchCurrentGpsLocation(context)
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        } else {
            val lat = viewModel.latitudeStr.toDoubleOrNull() ?: 41.0082
            val lon = viewModel.longitudeStr.toDoubleOrNull() ?: 28.9784
            viewModel.loadWeatherForLocation(lat, lon, viewModel.cityName)
        }
    }

    ForecastScreen(
        cityName = viewModel.cityName,
        subtitle = viewModel.subtitle,
        uiState = uiState,
        airQualityData = viewModel.airQualityData,
        favoriteCities = viewModel.favoriteCities,
        isFavorite = viewModel.isCurrentCityFavorite(),
        onToggleFavorite = { viewModel.toggleFavorite(context) },
        onRemoveFavorite = { city -> viewModel.removeFavorite(context, city) },
        onCitySelected = { city -> viewModel.selectFavoriteCity(city) },
        onBack = onBack,
        onRetry = viewModel::onRetry,
        onNavigateToCity = onNavigateToCity,
        modifier = modifier
    )
}

/**
 * Bir şehrin modern hava tahmini detay ekranı.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    cityName: String,
    subtitle: String,
    uiState: LoadState<Forecast>,
    airQualityData: AirQualityData?,
    favoriteCities: List<ForecastViewModel.FavoriteCity>,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRemoveFavorite: (ForecastViewModel.FavoriteCity) -> Unit,
    onCitySelected: (ForecastViewModel.FavoriteCity) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToCity: () -> Unit = onBack
) {
    val context = LocalContext.current

    val currentWeatherCode = if (uiState is LoadState.Success) {
        uiState.data.current.weatherCode
    } else 0

    // Bugünün indeksini tek bir yerde hesapla
    val todayIndex = remember(uiState) {
        if (uiState is LoadState.Success) {
            val todayDate = uiState.data.current.observedAt.toLocalDate()
            val idx = uiState.data.daily.indexOfFirst { it.date == todayDate }
            if (idx >= 0) idx else 0
        } else 0
    }

    val todayDaily = remember(uiState, todayIndex) {
        if (uiState is LoadState.Success) {
            uiState.data.daily.getOrNull(todayIndex)
        } else null
    }

    val isDaytime = remember(uiState, todayDaily) {
        if (uiState is LoadState.Success) {
            calculateIsDaytime(todayDaily, uiState.data.current.observedAt)
        } else {
            val hour = LocalDateTime.now().hour
            hour in 7..19
        }
    }

    val gradientColors = remember(currentWeatherCode, isDaytime) {
        getDynamicGradientColorsForWeather(currentWeatherCode, isDaytime)
    }
    val gradientBackground = Brush.verticalGradient(colors = gradientColors)

    val aiMessage = remember(uiState, todayDaily) {
        if (uiState is LoadState.Success) {
            generateAiSummaryMessage(uiState.data, todayDaily)
        } else ""
    }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.forLanguageTag("tr-TR")
            }
        }
        tts = ttsInstance

        onDispose {
            try {
                ttsInstance?.stop()
                ttsInstance?.shutdown()
            } catch (_: Exception) {}
        }
    }

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    val handleRootTap = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTapTime < 500) {
            tapCount++
        } else {
            tapCount = 1
        }
        lastTapTime = currentTime

        if (tapCount >= 3) {
            tapCount = 0
            if (tts != null && aiMessage.isNotEmpty()) {
                Toast.makeText(context, "AI Özeti Seslendiriliyor...", Toast.LENGTH_SHORT).show()
                tts?.speak(aiMessage, TextToSpeech.QUEUE_FLUSH, null, "AiSummaryTTS")
            }
        }
    }

    val displayName = cityName.ifEmpty { subtitle.ifEmpty { stringResource(R.string.detail_title) } }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Favorilerden Çıkar" else "Favorilere Ekle",
                                tint = if (isFavorite) Color(0xFFE53E3E) else Color.White
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCity,
                containerColor = Color.White.copy(alpha = 0.9f),
                contentColor = Color(0xFF1A365D),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = stringResource(R.string.search_hint)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = handleRootTap
                )
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is LoadState.Loading -> CircularProgressIndicator(color = Color.White)
                is LoadState.Empty -> ErrorView(message = "Bu şehir için tahmin bulunamadı.", onRetry = onRetry)
                is LoadState.Error -> ErrorView(message = uiState.message, onRetry = onRetry)
                is LoadState.Success -> {
                    ForecastContent(
                        cityName = cityName,
                        subtitle = subtitle,
                        forecast = uiState.data,
                        todayIndex = todayIndex,
                        todayDaily = todayDaily,
                        airQualityData = airQualityData,
                        favoriteCities = favoriteCities,
                        onRemoveFavorite = onRemoveFavorite,
                        onCitySelected = onCitySelected,
                        onNavigateToCity = onNavigateToCity
                    )
                }
            }
        }
    }
}

/**
 * Şehrin yerel gün doğumu/batımı ve gözlem saatine göre gündüz/gece hesabı yapar.
 */
fun calculateIsDaytime(dailyForecast: DailyForecast?, currentObservedAt: LocalDateTime? = null): Boolean {
    val sunrise = dailyForecast?.sunrise
    val sunset = dailyForecast?.sunset
    val now = currentObservedAt ?: LocalDateTime.now()
    if (sunrise != null && sunset != null) {
        val sunriseMinutes = sunrise.hour * 60 + sunrise.minute
        val sunsetMinutes = sunset.hour * 60 + sunset.minute
        val nowMinutes = now.hour * 60 + now.minute
        return nowMinutes in sunriseMinutes..sunsetMinutes
    }
    val hour = now.hour
    return hour in 7..19
}

/**
 * Hava durumu koduna ve anlık gündüz/gece durumuna göre dinamik arka plan renklerini seçer.
 */
private fun getDynamicGradientColorsForWeather(code: Int, isDaytime: Boolean): List<Color> {
    val condition = WeatherCondition.fromWeatherCode(code)
    return when {
        condition in listOf(WeatherCondition.CLEAR, WeatherCondition.MAINLY_CLEAR) -> {
            if (isDaytime) {
                listOf(Color(0xFFD69E2E), Color(0xFFDD6B20), Color(0xFF3182CE)) // Gündüz: Altın sarısı, açık turuncu, gök mavisi
            } else {
                listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF312E81)) // Gece: Derin gece mavisi, mor, koyu lacivert
            }
        }
        condition in listOf(WeatherCondition.RAIN, WeatherCondition.DRIZZLE, WeatherCondition.RAIN_SHOWERS, WeatherCondition.THUNDERSTORM) -> {
            if (isDaytime) {
                listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF1E293B)) // Yağmurlu Gündüz: Koyu çelik grisi, füme
            } else {
                listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF020617)) // Yağmurlu Gece: Derin fırtına mavisi/gece
            }
        }
        condition in listOf(WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS) -> {
            if (isDaytime) {
                listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF475569)) // Karlı Gündüz: Açık soğuk mavi, gümüş grisi
            } else {
                listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF0F172A)) // Karlı Gece: Koyulaştırılmış soğuk lacivert/füme
            }
        }
        else -> {
            if (isDaytime) {
                listOf(Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B)) // Bulutlu Gündüz: Puslu gri-mavi
            } else {
                listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF090D16)) // Bulutlu Gece: Derin gece grisi
            }
        }
    }
}

/**
 * Favori şehirler hızlı geçiş şeridi (LazyRow).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteCitiesBar(
    favoriteCities: List<ForecastViewModel.FavoriteCity>,
    selectedCityName: String,
    onCitySelected: (ForecastViewModel.FavoriteCity) -> Unit,
    onRemoveFavorite: (ForecastViewModel.FavoriteCity) -> Unit,
    onNavigateToCity: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cityToRemove by remember { mutableStateOf<ForecastViewModel.FavoriteCity?>(null) }

    if (cityToRemove != null) {
        AlertDialog(
            onDismissRequest = { cityToRemove = null },
            title = {
                Text(
                    text = "Favorilerden Çıkar",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "${cityToRemove?.name} favori şehirlerinizden çıkarılsın mı?",
                    color = Color.White.copy(alpha = 0.9f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        cityToRemove?.let { city ->
                            onRemoveFavorite(city)
                        }
                        cityToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53E3E),
                        contentColor = Color.White
                    )
                ) {
                    Text("Çıkar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { cityToRemove = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Text("İptal")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(favoriteCities, key = { it.name }) { city ->
            val isSelected = city.name.equals(selectedCityName, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.25f)),
                modifier = Modifier
                    .height(44.dp)
                    .combinedClickable(
                        onClick = { onCitySelected(city) },
                        onLongClick = { cityToRemove = city }
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📍",
                        fontSize = 14.sp
                    )
                    Text(
                        text = city.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }

        // "Şehir Ekle +" Glassmorphic Kartı
        item {
            Surface(
                onClick = onNavigateToCity,
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier.height(44.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "➕",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Şehir Ekle",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

/**
 * Tahmin ekranı içeriği.
 */
@Composable
private fun ForecastContent(
    cityName: String,
    subtitle: String,
    forecast: Forecast,
    todayIndex: Int,
    todayDaily: DailyForecast?,
    airQualityData: AirQualityData?,
    favoriteCities: List<ForecastViewModel.FavoriteCity>,
    onRemoveFavorite: (ForecastViewModel.FavoriteCity) -> Unit,
    onCitySelected: (ForecastViewModel.FavoriteCity) -> Unit,
    onNavigateToCity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FavoriteCitiesBar(
            favoriteCities = favoriteCities,
            selectedCityName = cityName,
            onCitySelected = onCitySelected,
            onRemoveFavorite = onRemoveFavorite,
            onNavigateToCity = onNavigateToCity
        )

        CurrentWeatherHeader(
            cityName = cityName,
            subtitle = subtitle,
            forecast = forecast,
            todayDaily = todayDaily
        )

        AiSummaryCard(forecast = forecast, todayDaily = todayDaily)

        AdviceCard(
            temp = forecast.current.temperatureC,
            weatherCode = forecast.current.weatherCode,
            windSpeed = forecast.current.windSpeedKmh,
            uvIndex = airQualityData?.uvIndex,
            aqi = airQualityData?.aqi
        )

        HourlyForecastSection(hours = forecast.hourly)

        SunArcCard(dailyForecast = todayDaily)

        // Bugünden itibaren tam 10 günü göster
        DailyForecastSection(daily = forecast.daily.drop(todayIndex).take(10))

        WeatherRadarPager(forecast = forecast, todayDaily = todayDaily, airQualityData = airQualityData)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Detay ekranı yatay radar/grafik kartları Pager'ı.
 */
@Composable
private fun WeatherRadarPager(
    forecast: Forecast,
    todayDaily: DailyForecast?,
    airQualityData: AirQualityData?,
    modifier: Modifier = Modifier
) {
    val current = forecast.current
    val precipProb = todayDaily?.precipitationProbability ?: 0

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title = "HAVA RADARI & DETAYLAR")

        val pagerState = rememberPagerState { 3 }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 4.dp),
            pageSpacing = 16.dp
        ) { page ->
            val indicatorText = "${pagerState.currentPage + 1} / 3"
            when (page) {
                0 -> {
                    // 1. Kart (Yağış ve Nem Radarı)
                    GlassCard(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💧 YAĞIŞ VE NEM RADARI",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.1.sp
                                    )
                                )
                                Text(
                                    text = indicatorText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = current.humidityPercent?.let { "%$it" } ?: "N/A",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Anlık Nem",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "%$precipProb",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            color = Color(0xFF90E0EF),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Yağış İhtimali",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                            }

                            Text(
                                text = if (precipProb > 50) "Bugün yağış bekleniyor, şemsiyenizi yanınıza alın." else "Bugün hava genel olarak yağışsız.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.8f)
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                1 -> {
                    // 2. Kart (Rüzgar ve Basınç Radarı)
                    GlassCard(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🌬️ RÜZGAR VE BASINÇ RADARI",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.1.sp
                                    )
                                )
                                Text(
                                    text = indicatorText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = current.windSpeedKmh?.let { "${it.roundToInt()} km/s" } ?: "N/A",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Rüzgar Hızı",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = windDirectionText(current.windDirectionDegrees),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = Color(0xFFFFD166),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Rüzgar Yönü",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = current.surfacePressureHpa?.let { "${it.roundToInt()} hPa" } ?: "1013 hPa",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = Color(0xFF90E0EF),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Basınç",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                            }

                            Text(
                                text = "Atmosferik basınç ve rüzgar seviyeleri izleniyor.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.8f)
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                2 -> {
                    AirQualityCard(airQuality = airQualityData)
                }
            }
        }
    }
}

private fun windDirectionText(degrees: Int?): String {
    if (degrees == null) return "Bilinmiyor"
    val directions = arrayOf("Kuzey", "Kuzey-Doğu", "Doğu", "Güneydoğu", "Güney", "Güney-Batı", "Batı", "Kuzey-Batı")
    val index = (((degrees + 22.5) % 360) / 45).toInt()
    return directions[index % 8]
}

/**
 * Header Bölümü: Şehir adı, büyük punto sıcaklık, hava durumu tanımı, H/L değerleri ve detay metrikleri.
 */
@Composable
private fun CurrentWeatherHeader(
    cityName: String,
    subtitle: String,
    forecast: Forecast,
    todayDaily: DailyForecast?,
    modifier: Modifier = Modifier
) {
    val current = forecast.current
    val condition = WeatherCondition.fromWeatherCode(current.weatherCode)

    val high = todayDaily?.maxTemperatureC ?: current.temperatureC
    val low = todayDaily?.minTemperatureC ?: current.temperatureC

    val displayName = cityName.ifEmpty { subtitle.ifEmpty { stringResource(R.string.detail_title) } }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = degrees(current.temperatureC),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 80.sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraLight
                ),
                textAlign = TextAlign.Center
            )
        }

        YesterdayBadge(currentTemp = current.temperatureC, hourly = forecast.hourly)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = condition.emoji,
                fontSize = 24.sp
            )
            Text(
                text = condition.label,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Text(
            text = "H:${degrees(high)}  L:${degrees(low)}",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (current.apparentTemperatureC != null || current.humidityPercent != null || current.windSpeedKmh != null) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    current.apparentTemperatureC?.let {
                        Metric(stringResource(R.string.detail_feels_like), degrees(it))
                    }
                    current.humidityPercent?.let {
                        Metric(stringResource(R.string.detail_humidity), "%$it")
                    }
                    current.windSpeedKmh?.let {
                        Metric(stringResource(R.string.detail_wind), "${it.roundToInt()} km/sa")
                    }
                }
            }
        }
    }
}

/**
 * Yapay Zeka Uyarı Kutusu (AI Summary Card)
 */
@Composable
private fun AiSummaryCard(
    forecast: Forecast,
    todayDaily: DailyForecast?,
    modifier: Modifier = Modifier
) {
    val message = remember(forecast, todayDaily) {
        generateAiSummaryMessage(forecast, todayDaily)
    }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✨", fontSize = 20.sp)
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Akıllı Özet",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Text(
                        text = "🔊 (3x Tıkla)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}

/**
 * Saatlik Tahmin Listesi (Hourly Forecast)
 */
@Composable
private fun HourlyForecastSection(
    hours: List<HourlyForecast>,
    modifier: Modifier = Modifier
) {
    val now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
    val upcoming = hours.filter { !it.time.isBefore(now) }.ifEmpty { hours }.take(HOURLY_COUNT)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title = stringResource(R.string.detail_hourly))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(items = upcoming, key = { it.time.toString() }) { hour ->
                    val isCurrentHour = !hour.time.isBefore(now) && hour.time.hour == now.hour

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isCurrentHour) stringResource(R.string.today) else hour.time.format(HOUR_FORMAT),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                        Text(
                            text = WeatherCondition.fromWeatherCode(hour.weatherCode).emoji,
                            fontSize = 22.sp
                        )
                        Text(
                            text = degrees(hour.temperatureC),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = percent(hour.precipitationProbability) ?: " ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF90E0EF),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 10 Günlük Tahmin Listesi (10-Day Forecast) - Daraltılabilir (Accordion) Yapıda
 */
@Composable
private fun DailyForecastSection(
    daily: List<DailyForecast>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val minWeekTemp = daily.mapNotNull { it.minTemperatureC }.minOrNull() ?: 0.0
    val maxWeekTemp = daily.mapNotNull { it.maxTemperatureC }.maxOrNull() ?: 40.0

    val visibleDays = if (isExpanded) daily else daily.take(3)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title = "10 GÜNLÜK TAHMİN")

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Column {
                visibleDays.forEachIndexed { index, day ->
                    if (index > 0) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    }
                    DailyRow(
                        day = day,
                        isToday = index == 0,
                        weekMinTemp = minWeekTemp,
                        weekMaxTemp = maxWeekTemp
                    )
                }

                if (daily.size > 3) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isExpanded) "Daha Az Göster ∧" else "10 Günlük Tahmini Gör ∨",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Günlük tahmin satırı
 */
@Composable
private fun DailyRow(
    day: DailyForecast,
    isToday: Boolean,
    weekMinTemp: Double,
    weekMaxTemp: Double,
    modifier: Modifier = Modifier
) {
    val conditionEmoji = day.weatherCode?.let { WeatherCondition.fromWeatherCode(it).emoji } ?: "🌡️"
    val minTempText = day.minTemperatureC?.let { degrees(it) } ?: "–"
    val maxTempText = day.maxTemperatureC?.let { degrees(it) } ?: "–"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isToday) stringResource(R.string.today) else day.date.format(SHORT_DAY_FORMAT).replaceFirstChar(Char::titlecase),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
            ),
            modifier = Modifier.width(60.dp)
        )

        Text(
            text = conditionEmoji,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(end = 6.dp)
        )

        Text(
            text = percent(day.precipitationProbability).orEmpty(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF90E0EF),
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = minTempText,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.75f),
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.End
        )

        TemperatureRangeBar(
            minTemp = day.minTemperatureC,
            maxTemp = day.maxTemperatureC,
            weekMinTemp = weekMinTemp,
            weekMaxTemp = weekMaxTemp,
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .padding(horizontal = 8.dp)
        )

        Text(
            text = maxTempText,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Günlük min-max sıcaklık aralığını çizen görsel çubuk.
 */
@Composable
private fun TemperatureRangeBar(
    minTemp: Double?,
    maxTemp: Double?,
    weekMinTemp: Double,
    weekMaxTemp: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barHeight = size.height
        val barWidth = size.width
        val cornerRadius = barHeight / 2

        drawRoundRect(
            color = Color.White.copy(alpha = 0.2f),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )

        if (minTemp != null && maxTemp != null) {
            val totalRange = (weekMaxTemp - weekMinTemp).coerceAtLeast(1.0)
            val startRatio = ((minTemp - weekMinTemp) / totalRange).toFloat().coerceIn(0f, 1f)
            val endRatio = ((maxTemp - weekMinTemp) / totalRange).toFloat().coerceIn(0f, 1f)

            val rangeGradient = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFFFD166),
                    Color(0xFFFF6B6B)
                )
            )

            val startX = barWidth * startRatio
            val calculatedWidth = barWidth * (endRatio - startRatio)
            val availableWidth = (barWidth - startX).coerceAtLeast(barHeight)
            val finalWidth = calculatedWidth.coerceAtLeast(barHeight).coerceAtMost(availableWidth)

            drawRoundRect(
                brush = rangeGradient,
                topLeft = Offset(startX, 0f),
                size = Size(finalWidth, barHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}

/**
 * Şeffaf (Glassmorphic) kart yapısı.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.White.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        content = content
    )
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.7f)
            )
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            color = Color.White.copy(alpha = 0.75f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        modifier = Modifier.padding(start = 4.dp)
    )
}

/**
 * Dinamik Yapay Zeka Özeti mesajı oluşturur.
 */
private fun generateAiSummaryMessage(forecast: Forecast, todayDaily: DailyForecast?): String {
    val condition = WeatherCondition.fromWeatherCode(forecast.current.weatherCode)
    val now = LocalDateTime.now()
    val nextRain = forecast.hourly
        .filter { !it.time.isBefore(now) }
        .firstOrNull { (it.precipitationProbability ?: 0) >= 30 || it.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95) }

    val high = todayDaily?.maxTemperatureC?.roundToInt() ?: forecast.current.temperatureC.roundToInt()
    val low = todayDaily?.minTemperatureC?.roundToInt() ?: forecast.current.temperatureC.roundToInt()

    return when {
        nextRain != null -> {
            val timeStr = nextRain.time.format(HOUR_FORMAT)
            "Saat $timeStr civarında ${WeatherCondition.fromWeatherCode(nextRain.weatherCode).label.lowercase()} hava bekleniyor. Şemsiyenizi yanınızda bulundurmanız iyi olabilir."
        }
        forecast.current.temperatureC >= 28 -> {
            "Bugün oldukça sıcak bir hava var (en yüksek $high°). Bol su içmeyi ve güneşten korunmayı unutmayın."
        }
        forecast.current.temperatureC <= 5 -> {
            "Bugün hava oldukça soğuk ($low° - $high°). Sıkı giyinmeyi ve sıcak kalmayı unutmayın."
        }
        else -> {
            "Şu an ${condition.label.lowercase()} hava hakim. Gün boyunca sıcaklıklar $low° ile $high° arasında seyredecek."
        }
    }
}

private fun degrees(celsius: Double) = "${celsius.roundToInt()}°"

private fun percent(value: Int?) = value?.takeIf { it > 0 }?.let { "%$it" }

private const val HOURLY_COUNT = 24
private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val SHORT_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("tr"))

@Preview(showBackground = true)
@Composable
private fun ForecastScreenPreview() {
    HavaTheme {
        ForecastScreen(
            cityName = "İstanbul",
            subtitle = "İstanbul, Türkiye",
            uiState = LoadState.Success(
                Forecast(
                    current = CurrentWeather(
                        temperatureC = 16.0,
                        weatherCode = 2,
                        observedAt = LocalDateTime.of(2026, 1, 15, 14, 0),
                        apparentTemperatureC = 15.0,
                        humidityPercent = 60,
                        windSpeedKmh = 14.0,
                        windDirectionDegrees = 180,
                        surfacePressureHpa = 1015.0
                    ),
                    hourly = List(12) { index ->
                        HourlyForecast(
                            time = LocalDateTime.now().plusHours(index.toLong()),
                            temperatureC = 16.0 + if (index < 6) index else (12 - index),
                            weatherCode = if (index == 3) 61 else 2,
                            precipitationProbability = if (index == 3) 70 else index * 5
                        )
                    },
                    daily = List(10) { index ->
                        DailyForecast(
                            date = LocalDate.of(2026, 1, 15).plusDays(index.toLong()),
                            minTemperatureC = 10.0 + (index % 3),
                            maxTemperatureC = 18.0 + (index % 4),
                            weatherCode = if (index == 2) 61 else 0,
                            precipitationProbability = if (index == 2) 80 else 10
                        )
                    }
                )
            ),
            airQualityData = AirQualityData(aqi = 35, uvIndex = 4.0, pm25 = 12.0, pm10 = 20.0),
            favoriteCities = listOf(
                ForecastViewModel.FavoriteCity("İstanbul", "İstanbul, Türkiye", 41.0082, 28.9784),
                ForecastViewModel.FavoriteCity("Ankara", "Ankara, Türkiye", 39.9334, 32.8597),
                ForecastViewModel.FavoriteCity("İzmir", "İzmir, Türkiye", 38.4237, 27.1428)
            ),
            isFavorite = true,
            onToggleFavorite = {},
            onRemoveFavorite = {},
            onCitySelected = {},
            onBack = {},
            onRetry = {}
        )
    }
}
