package com.kampplus.hava.ui.city

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.hava.R
import com.kampplus.hava.model.City
import com.kampplus.hava.model.WeatherCondition
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.theme.HavaTheme
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

private const val DEFAULT_TURKEY_LAT = 39.0
private const val DEFAULT_TURKEY_LNG = 35.0
private const val DEFAULT_TURKEY_ZOOM = 6.0

/**
 * OpenStreetMap (OsmDroid) tabanlı Şehir Seçici ekranının bağlayıcı katmanı.
 */
@Composable
fun CityRoute(
    onCityClick: (City) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    viewModel: CityViewModel = viewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CityScreen(
        query = query,
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::search,
        onCityClick = onCityClick,
        onBack = onBack,
        modifier = modifier
    )
}

/**
 * OpenStreetMap (OsmDroid) tabanlı Şehir Seçici ekranı.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityScreen(
    query: String,
    uiState: LoadState<List<City>>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onCityClick: (City) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedCity by remember { mutableStateOf<City?>(null) }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Radar katmanı durumları
    var radarEnabled by remember { mutableStateOf(false) }
    var radarOverlay by remember { mutableStateOf<TilesOverlay?>(null) }

    // Şehir sıcaklık etiketleri haritası (şehir adı -> sıcaklık)
    var cityTemperaturesMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // Seçili şehir anlık sıcaklık önbelleği (lat_lng -> Pair(sıcaklık, weatherCode)?)
    var selectedCityWeatherCache by remember { mutableStateOf<Map<String, Pair<Int, Int>?>>(emptyMap()) }
    var isSelectedCityWeatherLoading by remember { mutableStateOf(false) }

    // Marker bitmap önbelleği (key: "${temp}_${isSelected}" -> Pair(BitmapDrawable, Pair(anchorX, anchorY)))
    val pinCache = remember { mutableMapOf<String, Pair<BitmapDrawable, Pair<Float, Float>>>() }

    LaunchedEffect(radarEnabled) {
        if (radarEnabled && radarOverlay == null) {
            val overlay = withContext(Dispatchers.IO) {
                fetchRadarOverlay(context)
            }
            if (overlay != null) {
                radarOverlay = overlay
            } else {
                radarEnabled = false
                Toast.makeText(context, "Radar verisi alınamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.any { it }
        if (isGranted) {
            fetchUserLocation(context, locationManager, { isLocating = it }) { point ->
                userLocation = point
                point?.let {
                    mapViewRef?.controller?.animateTo(it, 15.0, 1000L)
                }
            }
        } else {
            Toast.makeText(context, "Konum izni gerekli", Toast.LENGTH_SHORT).show()
        }
    }

    val normalPinDrawable = remember(context) {
        BitmapDrawable(context.resources, createCustomPinBitmap(context, 0xFF2196F3.toInt(), 22))
    }
    val selectedPinDrawable = remember(context) {
        BitmapDrawable(context.resources, createCustomPinBitmap(context, 0xFFFF9800.toInt(), 30))
    }
    val userPinDrawable = remember(context) {
        BitmapDrawable(context.resources, createCustomPinBitmap(context, 0xFF4CAF50.toInt(), 26))
    }

    DisposableEffect(lifecycleOwner, mapViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val searchedCities = remember(uiState) {
        if (uiState is LoadState.Success) uiState.data else emptyList()
    }

    val displayCities = remember(searchedCities) {
        searchedCities.ifEmpty { DEFAULT_TURKEY_CITIES }
    }

    // Haritadaki ilk 20 şehir için toplu sıcaklık isteği at
    LaunchedEffect(displayCities) {
        try {
            cityTemperaturesMap = withContext(Dispatchers.IO) {
                CityTemperaturesRepository.fetchBatchTemperatures(displayCities)
            }
        } catch (e: Exception) {
            Log.e("CityScreen", "Batch sıcaklık verileri çekilemedi", e)
        }
    }

    // Seçili şehir değiştiğinde alt kart için anlık sıcaklık ve hava durumu çek
    LaunchedEffect(selectedCity) {
        selectedCity?.let { sel ->
            val key = "${sel.latitude}_${sel.longitude}"
            if (!selectedCityWeatherCache.containsKey(key)) {
                isSelectedCityWeatherLoading = true
                val result = withContext(Dispatchers.IO) {
                    CityTemperaturesRepository.fetchSingleWeather(sel.latitude, sel.longitude)
                }
                selectedCityWeatherCache = selectedCityWeatherCache + (key to result)
                isSelectedCityWeatherLoading = false
            }
        }
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. OsmDroid OpenStreetMap Canlı Harita Görünümü
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        // Koyu tema harita filtresi: renkleri ters çevir + doygunluğu azalt
                        val darkMatrix = ColorMatrix(
                            floatArrayOf(
                                -0.159f, -0.536f, -0.054f, 0f, 191f,
                                -0.181f, -0.608f, -0.061f, 0f, 217f,
                                -0.213f, -0.715f, -0.072f, 0f, 255f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                        overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(darkMatrix))
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        controller.setZoom(DEFAULT_TURKEY_ZOOM)
                        controller.setCenter(GeoPoint(DEFAULT_TURKEY_LAT, DEFAULT_TURKEY_LNG))
                        mapViewRef = this
                    }
                },
                update = { mapView ->
                    try {
                        mapView.overlays.clear()

                        val mapEventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                val lat = p.latitude
                                val lng = p.longitude
                                try {
                                    val (cityName, subtitle) = getLocationNameAndSubtitle(context, lat, lng)
                                    selectedCity = City(
                                        id = System.currentTimeMillis(),
                                        name = cityName,
                                        latitude = lat,
                                        longitude = lng,
                                        countryCode = null,
                                        country = subtitle,
                                        admin1 = cityName
                                    )
                                } catch (e: Exception) {
                                    Log.e("MapClick", "Error", e)
                                    selectedCity = City(
                                        id = System.currentTimeMillis(),
                                        name = "Seçilen Konum",
                                        latitude = lat,
                                        longitude = lng,
                                        countryCode = null,
                                        country = "${String.format(Locale.US, "%.2f", lat)}°N, ${String.format(Locale.US, "%.2f", lng)}°E",
                                        admin1 = "Seçilen Konum"
                                    )
                                }
                                return true
                            }
                            override fun longPressHelper(p: GeoPoint): Boolean = false
                        }
                        mapView.overlays.add(MapEventsOverlay(mapEventsReceiver))

                        // Yağış radarı katmanı (MapEventsOverlay eklendikten HEMEN SONRA ve şehir marker'larından ÖNCE)
                        if (radarEnabled) radarOverlay?.let { mapView.overlays.add(it) }

                        // Şehir İşaretçileri (Markers)
                        for (city in displayCities) {
                            val isSelected = selectedCity?.let {
                                abs(it.latitude - city.latitude) < 0.01 && abs(it.longitude - city.longitude) < 0.01
                            } == true
                            val temp = cityTemperaturesMap[city.name]

                            val (markerIconDrawable, anchorPair) = if (temp != null) {
                                val colorHex = if (isSelected) 0xFFFF9800.toInt() else 0xFF2196F3.toInt()
                                val dotSizeDp = if (isSelected) 30 else 22
                                val cacheKey = "${temp}_${isSelected}"

                                pinCache.getOrPut(cacheKey) {
                                    val (bitmap, anchor) = MarkerIcons.createTemperaturePinBitmap(context, colorHex, dotSizeDp, temp)
                                    BitmapDrawable(context.resources, bitmap) to anchor
                                }
                            } else {
                                val icon = if (isSelected) selectedPinDrawable else normalPinDrawable
                                icon to (0.5f to 0.5f)
                            }

                            val marker = Marker(mapView).apply {
                                position = GeoPoint(city.latitude, city.longitude)
                                title = city.name
                                snippet = city.subtitle
                                setAnchor(anchorPair.first, anchorPair.second)
                                icon = markerIconDrawable
                                infoWindow = null
                                setOnMarkerClickListener { _, _ ->
                                    selectedCity = city
                                    true
                                }
                            }
                            mapView.overlays.add(marker)
                        }

                        // Tıklanan özel konum İşaretçisi (etiketsiz kalacak)
                        selectedCity?.let { sel ->
                            val isAlreadyInList = displayCities.any {
                                it.name == sel.name && abs(it.latitude - sel.latitude) < 0.01
                            }
                            if (!isAlreadyInList) {
                                val selectedMarker = Marker(mapView).apply {
                                    position = GeoPoint(sel.latitude, sel.longitude)
                                    title = sel.name
                                    snippet = sel.subtitle
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    icon = selectedPinDrawable
                                    infoWindow = null
                                }
                                mapView.overlays.add(selectedMarker)
                            }
                        }

                        // Kullanıcı Konum İşaretçisi (etiketsiz kalacak)
                        userLocation?.let { loc ->
                            val userMarker = Marker(mapView).apply {
                                position = loc
                                title = "Konumum"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                icon = userPinDrawable
                                infoWindow = null
                            }
                            mapView.overlays.add(userMarker)
                        }

                        // Atıf (Copyright) Katmanı
                        mapView.overlays.add(CustomAttributionOverlay(context))

                        mapView.invalidate()
                    } catch (e: Exception) {
                        Log.e("OsmDroid", "Error adding markers", e)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Üst Katman: Şeffaf Arama Çubuğu ve Geri Butonu (Overlay)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }

                    SearchBar(
                        query = query,
                        onQueryChange = onQueryChange,
                        onSearch = onSearch,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Arama sonuçları açılır öneri kartı
                if (query.isNotEmpty() && uiState is LoadState.Success && uiState.data.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.height(200.dp),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            items(uiState.data) { city ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCity = city
                                            mapViewRef?.controller?.animateTo(
                                                GeoPoint(city.latitude, city.longitude),
                                                9.0,
                                                1000L
                                            )
                                            onQueryChange("")
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF62B6CB),
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Column {
                                        Text(
                                            text = city.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = city.subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Harita Zoom, Radar, Konumuma Git & Türkiye Odaklı Reset Butonları
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapTextControlButton(
                    text = "+",
                    onClick = { mapViewRef?.controller?.zoomIn() }
                )
                MapTextControlButton(
                    text = "−",
                    onClick = { mapViewRef?.controller?.zoomOut() }
                )

                // Yağış Radarı Katmanı Butonu
                Surface(
                    onClick = { radarEnabled = !radarEnabled },
                    shape = CircleShape,
                    color = Color(0xFF1E293B).copy(alpha = 0.85f),
                    contentColor = if (radarEnabled) Color(0xFF2196F3) else Color.White,
                    modifier = Modifier.size(44.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Radar",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Konumum Butonu
                Surface(
                    onClick = {
                        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fineGranted || coarseGranted) {
                            fetchUserLocation(context, locationManager, { isLocating = it }) { point ->
                                userLocation = point
                                point?.let {
                                    mapViewRef?.controller?.animateTo(it, 15.0, 1000L)
                                }
                            }
                        } else {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    },
                    shape = CircleShape,
                    color = Color(0xFF1E293B).copy(alpha = 0.85f),
                    contentColor = Color.White,
                    modifier = Modifier.size(44.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Konumum",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                MapTextControlButton(
                    text = "🇹🇷",
                    onClick = {
                        mapViewRef?.controller?.animateTo(
                            GeoPoint(DEFAULT_TURKEY_LAT, DEFAULT_TURKEY_LNG),
                            DEFAULT_TURKEY_ZOOM,
                            1000L
                        )
                    }
                )
            }

            // 4. Seçilen Şehir / Konum Alt Bilgi Kartı (Bottom Card)
            selectedCity?.let { city ->
                val cityKey = "${city.latitude}_${city.longitude}"
                val weatherData = selectedCityWeatherCache[cityKey]
                val isWeatherLoading = isSelectedCityWeatherLoading && weatherData == null

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = city.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            // Anlık Sıcaklık ve İkon Satırı
                            if (isWeatherLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else if (weatherData != null) {
                                val (temp, code) = weatherData
                                val condition = WeatherCondition.fromWeatherCode(code)
                                Text(
                                    text = "${condition.emoji} $temp° · ${condition.label}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF90E0EF),
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Text(
                                text = city.subtitle.ifEmpty {
                                    "${String.format(Locale.US, "%.2f", city.latitude)}°N, ${String.format(Locale.US, "%.2f", city.longitude)}°E"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.75f)
                                ),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Button(
                            onClick = { onCityClick(city) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3182CE),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Text(
                                text = "Hava Durumunu Gör",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchUserLocation(
    context: Context,
    locationManager: LocationManager,
    isLocatingSetter: (Boolean) -> Unit,
    onLocationFound: (GeoPoint?) -> Unit
) {
    isLocatingSetter(true)
    try {
        var bestLocation: Location? = null
        val providers = locationManager.getProviders(true)
        for (provider in providers) {
            val loc = locationManager.getLastKnownLocation(provider)
            if (loc != null) {
                if (bestLocation == null || loc.time > bestLocation.time) {
                    bestLocation = loc
                }
            }
        }

        if (bestLocation != null) {
            isLocatingSetter(false)
            onLocationFound(GeoPoint(bestLocation.latitude, bestLocation.longitude))
        } else {
            var requested = false
            for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                if (locationManager.isProviderEnabled(provider)) {
                    try {
                        @Suppress("DEPRECATION")
                        locationManager.requestSingleUpdate(provider, { location ->
                            isLocatingSetter(false)
                            onLocationFound(GeoPoint(location.latitude, location.longitude))
                        }, Looper.getMainLooper())
                        requested = true
                        break
                    } catch (_: Exception) {}
                }
            }

            if (!requested) {
                isLocatingSetter(false)
                Toast.makeText(context, "Konum alınamadı, konum servisini açın", Toast.LENGTH_SHORT).show()
                onLocationFound(null)
            }
        }
    } catch (_: SecurityException) {
        isLocatingSetter(false)
        Toast.makeText(context, "Konum izni gerekli", Toast.LENGTH_SHORT).show()
        onLocationFound(null)
    } catch (e: Exception) {
        isLocatingSetter(false)
        Toast.makeText(context, "Konum alınamadı, konum servisini açın", Toast.LENGTH_SHORT).show()
        onLocationFound(null)
    }
}

private fun createCustomPinBitmap(context: Context, colorHex: Int, sizeDp: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = sizePx / 2f

    val haloPaint = Paint().apply {
        color = colorHex
        alpha = (255 * 0.25f).toInt()
        isAntiAlias = true
    }
    canvas.drawCircle(radius, radius, radius, haloPaint)

    val innerRadius = radius * 0.7f
    val circlePaint = Paint().apply {
        color = colorHex
        isAntiAlias = true
    }
    canvas.drawCircle(radius, radius, innerRadius, circlePaint)

    val borderWidth = 3f * density
    val borderPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
        isAntiAlias = true
    }
    canvas.drawCircle(radius, radius, innerRadius - borderWidth / 2, borderPaint)

    return bitmap
}

/**
 * Şeffaf arama kutusu bileşeni.
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                stringResource(R.string.search_hint),
                color = Color.White.copy(alpha = 0.6f)
            )
        },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear), tint = Color.White)
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.85f),
            unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.85f),
            focusedBorderColor = Color.White.copy(alpha = 0.4f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(24.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                onSearch()
            }
        )
    )
}

/**
 * Harita üzerindeki butonlar (+ / - / Reset).
 */
@Composable
private fun MapTextControlButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFF1E293B).copy(alpha = 0.85f),
        contentColor = Color.White,
        modifier = Modifier.size(44.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

private fun getLocationNameAndSubtitle(context: Context, lat: Double, lng: Double): Pair<String, String> {
    try {
        if (Geocoder.isPresent()) {
            val geocoder = Geocoder(context, Locale.forLanguageTag("tr-TR"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val cityName = address.locality ?: address.subAdminArea ?: address.adminArea ?: address.featureName
                val countryName = address.countryName

                val name = cityName ?: countryName ?: "Seçilen Konum"
                val subtitle = listOfNotNull(address.adminArea, countryName).distinct().joinToString(", ")
                if (name.isNotEmpty()) {
                    return name to subtitle
                }
            }
        }
    } catch (_: Exception) {}

    // Fallback to known cities
    val knownCity = getClosestKnownCity(lat, lng)
    if (knownCity != null) {
        return knownCity to "Türkiye"
    }

    val countryHint = if (lat in 36.0..42.0 && lng in 26.0..45.0) "Türkiye" else "Dünya Konumu"
    return "Seçilen Bölge" to countryHint
}

private fun getClosestKnownCity(lat: Double, lng: Double): String? {
    val knownCities = listOf(
        "İstanbul" to (41.0082 to 28.9784),
        "Ankara" to (39.9334 to 32.8597),
        "İzmir" to (38.4237 to 27.1428),
        "Bursa" to (40.1885 to 29.0610),
        "Antalya" to (36.8969 to 30.7133),
        "Adana" to (37.0000 to 35.3213),
        "Trabzon" to (41.0027 to 39.7168),
        "Erzurum" to (39.9043 to 41.2679),
        "Gaziantep" to (37.0662 to 37.3833),
        "Konya" to (37.8746 to 32.4932),
        "Kayseri" to (38.7312 to 35.4787),
        "Eskişehir" to (39.7767 to 30.5206),
        "Samsun" to (41.2867 to 36.3300),
        "Diyarbakır" to (37.9144 to 40.2306),
        "Muğla" to (37.2153 to 28.3636),
        "Londra" to (51.5074 to -0.1278),
        "Paris" to (48.8566 to 2.3522),
        "Berlin" to (52.5200 to 13.4050),
        "New York" to (40.7128 to -74.0060),
        "Roma" to (41.9028 to 12.4964),
        "Tokyo" to (35.6762 to 139.6503)
    )

    for ((name, coords) in knownCities) {
        val dLat = lat - coords.first
        val dLng = lng - coords.second
        val dist = dLat * dLat + dLng * dLng
        if (dist < 1.5) {
            return name
        }
    }
    return null
}

private val DEFAULT_TURKEY_CITIES = listOf(
    City(1L, "İstanbul", 41.0082, 28.9784, "TR", "Türkiye", "İstanbul"),
    City(2L, "Ankara", 39.9334, 32.8597, "TR", "Türkiye", "Ankara"),
    City(3L, "İzmir", 38.4237, 27.1428, "TR", "Türkiye", "İzmir"),
    City(4L, "Bursa", 40.1885, 29.0610, "TR", "Türkiye", "Bursa"),
    City(5L, "Antalya", 36.8969, 30.7133, "TR", "Türkiye", "Antalya"),
    City(6L, "Adana", 37.0000, 35.3213, "TR", "Türkiye", "Adana"),
    City(7L, "Trabzon", 41.0027, 39.7168, "TR", "Türkiye", "Trabzon"),
    City(8L, "Erzurum", 39.9043, 41.2679, "TR", "Türkiye", "Erzurum"),
    City(9L, "Gaziantep", 37.0662, 37.3833, "TR", "Türkiye", "Gaziantep"),
    City(10L, "Konya", 37.8746, 32.4932, "TR", "Türkiye", "Konya")
)

@Preview(showBackground = true)
@Composable
private fun CityScreenPreview() {
    HavaTheme {
        CityScreen(
            query = "Ankara",
            uiState = LoadState.Success(
                listOf(
                    City(323786, "Ankara", 39.91987, 32.85427, "TR", "Türkiye", "Ankara")
                )
            ),
            onQueryChange = {},
            onSearch = {},
            onCityClick = {}
        )
    }
}
