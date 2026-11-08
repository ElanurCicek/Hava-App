package com.kampplus.hava.ui.forecast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kampplus.hava.model.HourlyForecast
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun YesterdayBadge(
    currentTemp: Double,
    hourly: List<HourlyForecast>,
    modifier: Modifier = Modifier
) {
    val now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
    val yesterdaySameTime = now.minusDays(1)

    val yesterdayItem = hourly.firstOrNull { it.time.truncatedTo(ChronoUnit.HOURS) == yesterdaySameTime }
    if (yesterdayItem == null) return

    val diff = (currentTemp - yesterdayItem.temperatureC).roundToInt()
    if (abs(diff) < 1) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.2f),
            modifier = modifier
        ) {
            Text(
                text = "Dünle aynı",
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    } else {
        val isWarmer = diff > 0
        val text = if (isWarmer) "Dünden ${abs(diff)}°C daha sıcak" else "Dünden ${abs(diff)}°C daha soğuk"
        val badgeColor = if (isWarmer) Color(0xFFDD6B20).copy(alpha = 0.6f) else Color(0xFF3182CE).copy(alpha = 0.6f)

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = badgeColor,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            modifier = modifier
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
