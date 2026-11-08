package com.kampplus.hava.ui.forecast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdviceCard(
    temp: Double,
    weatherCode: Int,
    windSpeed: Double?,
    uvIndex: Double?,
    aqi: Int?,
    modifier: Modifier = Modifier
) {
    val advices = remember(temp, weatherCode, windSpeed, uvIndex, aqi) {
        generateAdvice(temp, weatherCode, windSpeed, uvIndex, aqi)
    }

    if (advices.isEmpty()) return

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "💡 AKILLI ÖNERİ & GİYİM",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (advice in advices) {
                    Text(
                        text = advice,
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
}
