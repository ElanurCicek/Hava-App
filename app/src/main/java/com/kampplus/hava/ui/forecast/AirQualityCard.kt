package com.kampplus.hava.ui.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kampplus.hava.model.AirQualityData

@Composable
fun AirQualityCard(
    airQuality: AirQualityData?,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth().height(160.dp)) {
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
                    text = "🍃 HAVA KALİTESİ & UV",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                )
                Text(
                    text = "3 / 3",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (airQuality == null || (airQuality.aqi == null && airQuality.uvIndex == null)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Veri yok",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f))
                    )
                }
            } else {
                val aqi = airQuality.aqi ?: 0
                val (aqiLabel, aqiColor) = getAqiCategory(aqi)
                val uv = airQuality.uvIndex ?: 0.0
                val (uvLabel, uvAdvice) = getUvCategory(uv)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (airQuality.aqi != null) "$aqi ($aqiLabel)" else "Veri yok",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = aqiColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "AQI | PM2.5: ${airQuality.pm25 ?: "-"} | PM10: ${airQuality.pm10 ?: "-"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "UV: $uv ($uvLabel)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFFFFD166),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = uvAdvice,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    val progress = (aqi / 120f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .background(aqiColor)
                    )
                }
            }
        }
    }
}

private fun getAqiCategory(aqi: Int): Pair<String, Color> {
    return when {
        aqi <= 20 -> "Çok iyi" to Color(0xFF48BB78)
        aqi <= 40 -> "İyi" to Color(0xFF68D391)
        aqi <= 60 -> "Orta" to Color(0xFFECC94B)
        aqi <= 80 -> "Kötü" to Color(0xFFED8936)
        aqi <= 100 -> "Çok kötü" to Color(0xFFE53E3E)
        else -> "Tehlikeli" to Color(0xFF9B2C2C)
    }
}

private fun getUvCategory(uv: Double): Pair<String, String> {
    return when {
        uv <= 2.0 -> "Düşük" to "Güneş kremi gerekmez"
        uv <= 5.0 -> "Orta" to "Güneş gözlüğü tak"
        uv <= 7.0 -> "Yüksek" to "Güneş kremi sür"
        uv <= 10.0 -> "Çok yüksek" to "Güneş kremi şart"
        else -> "Aşırı" to "Güneşten kaçının"
    }
}
